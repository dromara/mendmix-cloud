package com.jeesuite.kafka.consumer;

import java.io.Closeable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.kafka.handler.MessageHandler;
import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.monitor.KafkaConsumerCommand;
import com.jeesuite.kafka.monitor.model.ConsumerGroupInfo;
import com.jeesuite.kafka.monitor.model.TopicInfo;
import com.jeesuite.kafka.monitor.model.TopicPartitionInfo;
import com.jeesuite.kafka.thread.StandardThreadExecutor;

/**
 * 默认消费者实现（new consumer api）
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月12日
 */
public class NewApiTopicConsumer implements TopicConsumer,Closeable {

	private static final Logger logger = LoggerFactory.getLogger(ConsumerWorker.class);

	private Map<String, MessageHandler> topicHandlers;
	
	private ExecutorService fetcheExecutor;
	private StandardThreadExecutor processExecutor;
	private List<ConsumerWorker> consumerWorks = new ArrayList<>();

	private KafkaConsumer<String, Serializable> consumer;
	
	private ErrorMessageDefaultProcessor errorMessageProcessor = new ErrorMessageDefaultProcessor(1);
	
	private final Map<TopicPartition, Long> partitionToUncommittedOffsetMap = new ConcurrentHashMap<>();
	private final List<Future<Boolean>> committedOffsetFutures = new ArrayList<>();
	
	private boolean offsetAutoCommit;
	private ConsumerContext consumerContext;
	
	public NewApiTopicConsumer(ConsumerContext context) {
		super();
		this.consumerContext = context;
		this.topicHandlers = context.getMessageHandlers();
		//
	    fetcheExecutor = Executors.newFixedThreadPool(topicHandlers.size());
	    //
	    processExecutor = new StandardThreadExecutor(1, context.getMaxProcessThreads(), 1000);
	    //enable.auto.commit 默认为true
	    offsetAutoCommit = context.getProperties().containsKey("enable.auto.commit") == false || Boolean.parseBoolean(context.getProperties().getProperty("enable.auto.commit"));
	}

	@Override
	public void start() {
		createKafkaConsumer();
		//重置offset
		if(consumerContext.getOffsetLogHanlder() != null){	
			resetCorrectOffsets();
		}
		//按主题数创建ConsumerWorker线程
		for (int i = 0; i < topicHandlers.size(); i++) {
			ConsumerWorker consumer = new ConsumerWorker();
			consumerWorks.add(consumer);
			fetcheExecutor.submit(consumer);
		}
	}

	/**
	 * 按上次记录重置offsets
	 */
	private void resetCorrectOffsets() {
		
		KafkaConsumerCommand consumerCommand = new KafkaConsumerCommand(consumerContext.getProperties().getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
		try {
			List<TopicInfo> topicInfos = consumerCommand.consumerGroup(consumerContext.getGroupId()).getTopics();
			for (TopicInfo topic : topicInfos) {
				List<TopicPartitionInfo> partitions = topic.getPartitions();
				for (TopicPartitionInfo partition : partitions) {	
					try {						
						//期望的偏移
						long expectOffsets = consumerContext.getLatestProcessedOffsets(topic.getTopicName(), partition.getPartition());
						//
						if(expectOffsets < partition.getOffset()){						
							consumer.seek(new TopicPartition(topic.getTopicName(), partition.getPartition()), expectOffsets);
							logger.info("seek Topic[{}] partition[{}] from {} to {}",topic.getTopicName(), partition.getPartition(),partition.getOffset(),expectOffsets);
						}
					} catch (Exception e) {
						logger.warn("try seek topic["+topic.getTopicName()+"] partition["+partition.getPartition()+"] offsets error",e);
					}
				}
			}
			
		} catch (Exception e) {
			logger.warn("KafkaConsumerCommand.consumerGroup("+consumerContext.getGroupId()+") error",e);
		}
		consumerCommand.close();
	}

	@Override
	public void close() {
		for (int i = 0; i < consumerWorks.size(); i++) {
			consumerWorks.get(i).close();
			consumerWorks.remove(i);
			i--;
		}
		fetcheExecutor.shutdown();
		processExecutor.shutdown();
		errorMessageProcessor.close();
		consumer.close();
	}
	
	private <K extends Serializable, V extends Serializable> void createKafkaConsumer(){
		consumer = new KafkaConsumer<>(consumerContext.getProperties());
		ConsumerRebalanceListener listener = new ConsumerRebalanceListener() {

			@Override
			public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
				if (!committedOffsetFutures.isEmpty())
					committedOffsetFutures.get(0).cancel(true);

				commitOffsets(partitionToUncommittedOffsetMap);
			}

			@Override
			public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
				for (TopicPartition tp : partitions) {
					OffsetAndMetadata offsetAndMetaData = consumer.committed(tp);
					long startOffset = offsetAndMetaData != null ? offsetAndMetaData.offset() : -1L;
					logger.debug("Assigned topicPartion : {} offset : {}", tp, startOffset);

					if (startOffset >= 0)
						consumer.seek(tp, startOffset);
				}
			}
		};

		List<String> topics = new ArrayList<>(topicHandlers.keySet());
		
		if(offsetAutoCommit){			
			consumer.subscribe(topics);
		}else{
			consumer.subscribe(topics, listener);
		}
	}
	
	
	private void commitOffsets(Map<TopicPartition, Long> partitionToOffsetMap) {

		if (!partitionToOffsetMap.isEmpty()) {
			Map<TopicPartition, OffsetAndMetadata> partitionToMetadataMap = new HashMap<>();
			for (Entry<TopicPartition, Long> e : partitionToOffsetMap.entrySet()) {
				partitionToMetadataMap.put(e.getKey(), new OffsetAndMetadata(e.getValue() + 1));
			}

			logger.debug("committing the offsets : {}", partitionToMetadataMap);
			consumer.commitSync(partitionToMetadataMap);
			partitionToOffsetMap.clear();
		}
	}
	
	private class ConsumerWorker implements Runnable {

		private AtomicBoolean closed = new AtomicBoolean();

		@Override
		public void run() {

			ExecutorService executor = Executors.newFixedThreadPool(1);

			while (!closed.get()) {
				ConsumerRecords<String,Serializable> records = consumer.poll(1500);
				// no record found
				if (records.isEmpty()) {
					continue;
				}
				
				if(offsetAutoCommit){
					for (final ConsumerRecord<String,Serializable> record : records) {	
						
						processConsumerRecords(record);
					}
					continue;
				}

				//由于处理消息可能产生延时，收到消息后，暂停所有分区并手动发送心跳，以避免consumer group被踢掉
				consumer.pause(consumer.assignment());
				Future<Boolean> future = executor.submit(new ConsumeRecords(records, partitionToUncommittedOffsetMap));
				committedOffsetFutures.add(future);

				Boolean isCompleted = false;
				while (!isCompleted && !closed.get()) {
					try {
						//等待 heart-beat 间隔时间
						isCompleted = future.get(3, TimeUnit.SECONDS); 
					} catch (TimeoutException e) {
						logger.debug("heartbeats the coordinator");
						consumer.poll(0); // does heart-beat
						commitOffsets(partitionToUncommittedOffsetMap);
					} catch (CancellationException e) {
						logger.debug("ConsumeRecords Job got cancelled");
						break;
					} catch (ExecutionException | InterruptedException e) {
						logger.error("Error while consuming records", e);
						break;
					}
				}
				committedOffsetFutures.remove(future);
				consumer.resume(consumer.assignment());
				commitOffsets(partitionToUncommittedOffsetMap);
			}

			try {
				executor.shutdownNow();
				while (!executor.awaitTermination(5, TimeUnit.SECONDS))
					;
			} catch (InterruptedException e) {
				logger.error("Error while exiting the consumer");
			}
			consumer.close();
			logger.info("consumer exited");
		}
		
		
		/**
		 * @param record
		 */
		private void processConsumerRecords(final ConsumerRecord<String, Serializable> record) {
			final MessageHandler messageHandler = topicHandlers.get(record.topic());
			
			consumerContext.saveOffsetsBeforeProcessed(record.topic(), record.partition(), record.offset());
			//兼容没有包装的情况
			final DefaultMessage message = record.value() instanceof DefaultMessage ? (DefaultMessage) record.value() : new DefaultMessage((Serializable) record.value());
			//第一阶段处理
			messageHandler.p1Process(message);
			//第二阶段处理
			processExecutor.submit(new Runnable() {
				@Override
				public void run() {
					try {									
						messageHandler.p2Process(message);
						//
						consumerContext.saveOffsetsAfterProcessed(record.topic(), record.partition(), record.offset());
					} catch (Exception e) {
						boolean processed = messageHandler.onProcessError(message);
						if(processed == false){
							errorMessageProcessor.submit(message, messageHandler);
						}
						logger.error("["+messageHandler.getClass().getSimpleName()+"] process Topic["+record.topic()+"] error",e);
					}
				}
			});
		}

		public void close() {
			closed.set(true);
		}

		private class ConsumeRecords implements Callable<Boolean> {

			ConsumerRecords<String,Serializable> records;
			Map<TopicPartition, Long> partitionToUncommittedOffsetMap;

			public ConsumeRecords(ConsumerRecords<String,Serializable> records,
					Map<TopicPartition, Long> partitionToUncommittedOffsetMap) {
				this.records = records;
				this.partitionToUncommittedOffsetMap = partitionToUncommittedOffsetMap;
			}

			@Override
			public Boolean call() {

				logger.debug("Number of records received : {}", records.count());
				try {
					for (final ConsumerRecord<String,Serializable> record : records) {
						TopicPartition tp = new TopicPartition(record.topic(), record.partition());
						logger.info("Record received topicPartition : {}, offset : {}", tp,record.offset());
						partitionToUncommittedOffsetMap.put(tp, record.offset());
						
						processConsumerRecords(record);
					}
				} catch (Exception e) {
					logger.error("Error while consuming", e);
				}
				return true;
			}
		}

	}

}
