package com.jeesuite.kafka.consumer;

import java.io.Closeable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.kafka.handler.MessageHandler;
import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.thread.StandardThreadExecutor;
import com.jeesuite.kafka.thread.StandardThreadExecutor.StandardThreadFactory;

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
	private ErrorMessageDefaultProcessor errorMessageProcessor = new ErrorMessageDefaultProcessor(1);
	
	private final Map<String, OffsetCommitMeta> uncommittedOffsetMap = new ConcurrentHashMap<>();
	
	private boolean offsetAutoCommit;
	private ConsumerContext consumerContext;
	
	private Properties properties;
    private String clientIdPrefix;
    private long pollTimeout = 1000;
    
    AtomicBoolean commiting = new AtomicBoolean(false);
    
    private ReentrantLock lock = new ReentrantLock();
    
    
	public NewApiTopicConsumer(ConsumerContext context) {
		this.consumerContext = context;
		properties = context.getProperties();
		clientIdPrefix = properties.getProperty(ConsumerConfig.CLIENT_ID_CONFIG);
		this.topicHandlers = context.getMessageHandlers();
		//enable.auto.commit 默认为true
		offsetAutoCommit = context.getProperties().containsKey("enable.auto.commit") == false || Boolean.parseBoolean(context.getProperties().getProperty("enable.auto.commit"));
		//
	    fetcheExecutor = Executors.newFixedThreadPool(topicHandlers.size(),new StandardThreadFactory("KafkaFetcher"));
	    //
	    processExecutor = new StandardThreadExecutor(1, context.getMaxProcessThreads(), 1000,new StandardThreadFactory("KafkaProcessor"));
	    
	    if(properties.containsKey("poll.timeout.ms")){	    	
	    	pollTimeout = Long.parseLong(properties.remove("poll.timeout.ms").toString());
	    }else{
	    	pollTimeout = Long.parseLong(ResourceUtils.getProperty("consumer.poll.timeout.ms", "1000"));
	    }
	    logger.info("pollTimeout:"+pollTimeout);
	}

	@Override
	public void start() {
		//按主题数创建ConsumerWorker线程
		List<String> topics = new ArrayList<>(topicHandlers.keySet());
		for (int i = 0; i < topics.size(); i++) {
			ConsumerWorker worker = new ConsumerWorker(topics.get(i),i);
			//
			subscribeTopic(worker,topics.get(i));
			//重置offset
			if(offsetAutoCommit && consumerContext.getOffsetLogHanlder() != null){	
			   resetCorrectOffsets(worker);
			}
			consumerWorks.add(worker);
			fetcheExecutor.submit(worker);
		}
	}
	
	/**
	 * 按上次记录重置offsets
	 */
	private void resetCorrectOffsets(ConsumerWorker worker) {	
		
		KafkaConsumer<String, Serializable> consumer = worker.consumer;
		Map<String, List<PartitionInfo>> topicInfos = consumer.listTopics();
		Set<String> topics = topicInfos.keySet();
		
		List<String> expectTopics = new ArrayList<>(topicHandlers.keySet());
		
		List<PartitionInfo> patitions = null;
		
		consumer.poll(200);
		
		for (String topic : topics) {
			if(!expectTopics.contains(topic))continue;
			
			patitions = topicInfos.get(topic);
			for (PartitionInfo partition : patitions) {
				try {						
					//期望的偏移
					long expectOffsets = consumerContext.getLatestProcessedOffsets(topic, partition.partition());
					//
					TopicPartition topicPartition = new TopicPartition(partition.topic(), partition.partition());
					OffsetAndMetadata metadata = consumer.committed(topicPartition);
					
					Set<TopicPartition> assignment = consumer.assignment();
					if(assignment.contains(topicPartition)){
						if(expectOffsets >= 0 && expectOffsets < metadata.offset()){								
							consumer.seek(topicPartition, expectOffsets);
							//consumer.seekToBeginning(assignment);
					        logger.info(">>>>>>> seek Topic[{}] partition[{}] from {} to {}",topic, partition.partition(),metadata.offset(),expectOffsets);
						}
					}
				} catch (Exception e) {
					logger.warn("try seek topic["+topic+"] partition["+partition.partition()+"] offsets error");
				}
			}
		}
		consumer.resume(consumer.assignment());
	}

	
	
	private <K extends Serializable, V extends Serializable> void subscribeTopic(ConsumerWorker worker,String topic){

		List<String> topics = new ArrayList<>(Arrays.asList(topic));
		
		if(offsetAutoCommit){			
			worker.consumer.subscribe(topics);
		}else{
			ConsumerRebalanceListener listener = new ConsumerRebalanceListener() {
				@Override
				public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
					worker.assigndPartitions.clear();
					for (TopicPartition tp : partitions) {
						worker.assigndPartitions.add(tp);
						//TODO 
					}
				}
				@Override
				public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
					//
					worker.assigndPartitions.clear();
					for (TopicPartition tp : partitions) {
						//期望的偏移
						long startOffset = 0L;
	                    if(consumerContext.getOffsetLogHanlder() != null){	
							try {
								startOffset = consumerContext.getLatestProcessedOffsets(tp.topic(), tp.partition());
								logger.info("offsetLogHanlder.getLatestProcessedOffsets({},{}) result is {}",tp.topic(), tp.partition(),startOffset);
							} catch (Exception e) {
								logger.warn("offsetLogHanlder.getLatestProcessedOffsets error:{}",e.getMessage());
							}
						}
	                    if(startOffset == 0){
	                    	OffsetAndMetadata offsetAndMetaData = worker.consumer.committed(tp);
	                    	startOffset = offsetAndMetaData != null ? offsetAndMetaData.offset() : -1L;
	                    }
						
						if (startOffset >= 0){
							worker.consumer.seek(tp, startOffset);
						}
						//
						
						worker.assigndPartitions.add(tp);
						OffsetCommitMeta meta = new OffsetCommitMeta(worker.consumer,tp, startOffset);
						uncommittedOffsetMap.put(meta.getKey(), meta);
						
						logger.debug("Assigned topicPartion : {} offset : {}", tp, startOffset);
					}
				}
			};
			//
			worker.consumer.subscribe(topics, listener);
			//提交分区
			commitOffsets(worker.consumer);
		}
	}
	
	
	
	private Map<TopicPartition, OffsetAndMetadata> partitionToMetadataMap = new HashMap<>();
	
	private void commitOffsets(KafkaConsumer<String, Serializable> consumer) {
		if(commiting.get())return;
		commiting.set(true);
		lock.lock();
		try {
			for (OffsetCommitMeta m : uncommittedOffsetMap.values()) {
				if (m.isCommitd())continue;
				if (!m.getConsumer().equals(consumer))continue;
				partitionToMetadataMap.put(m.getPartition(), new OffsetAndMetadata(m.getOffset()));
			}
			if(partitionToMetadataMap.isEmpty())return ;
			
			logger.debug("committing the offsets : {}", partitionToMetadataMap);
			consumer.commitAsync(partitionToMetadataMap, new OffsetCommitCallback() {
				@Override
				public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
					if(exception == null){
						logger.debug("committed the offsets : {}",offsets);
						for (OffsetCommitMeta m : uncommittedOffsetMap.values()) {
							m.setCommitd(true);
						}
					}else{
						logger.error("committ the offsets error",exception);
					}
				}
			});
		} finally {
			lock.unlock();
			partitionToMetadataMap.clear();
			commiting.set(false);
		}
	}
	
	@Override
	public void close() {
		for (int i = 0; i < consumerWorks.size(); i++) {
			consumerWorks.get(i).close();
			consumerWorks.remove(i);
			i--;
		}
		fetcheExecutor.shutdown();
		if(processExecutor != null)processExecutor.shutdown();
		errorMessageProcessor.close();
	}
	
	private class ConsumerWorker implements Runnable {

		private AtomicBoolean closed = new AtomicBoolean();
		KafkaConsumer<String, Serializable> consumer;
		private List<TopicPartition> assigndPartitions = new ArrayList<>();
		
		
		public ConsumerWorker(String topic,int index) {
			properties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, clientIdPrefix + "_" + index);
			consumer = new KafkaConsumer<String, Serializable>(properties);
		}
		

		@Override
		public void run() {

			ExecutorService executor = Executors.newFixedThreadPool(1);

			while (!closed.get()) {
				ConsumerRecords<String,Serializable> records = null;
				records = consumer.poll(pollTimeout);
				// no record found
				if (records.isEmpty()) {
					continue;
				}
				
				for (final ConsumerRecord<String,Serializable> record : records) {	
					processConsumerRecords(record);
				}

				//提交分区
			   if(!offsetAutoCommit){
					//由于处理消息可能产生延时，收到消息后，暂停所有分区并手动发送心跳，以避免consumer group被踢掉
//					consumer.pause(consumer.assignment());
//					Future<Boolean> future = executor.submit(new ConsumeRecords(records));
//					committedOffsetFutures.add(future);
	//
//					Boolean isCompleted = false;
//					while (!isCompleted && !closed.get()) {
//						try {
//							//等待 heart-beat 间隔时间
//							isCompleted = future.get(3, TimeUnit.SECONDS); 
//						} catch (TimeoutException e) {
//							logger.debug("heartbeats the coordinator");
//							consumer.poll(0); // does heart-beat
//							commitOffsets(topic,consumer,partitionToUncommittedOffsetMap);
//						} catch (CancellationException e) {
//							logger.debug("ConsumeRecords Job got cancelled");
//							break;
//						} catch (ExecutionException | InterruptedException e) {
//							logger.error("Error while consuming records", e);
//							break;
//						}
//					}
//					committedOffsetFutures.remove(future);
//					consumer.resume(consumer.assignment());
				   
				 //提交分区
				  commitOffsets(consumer);
			   }
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
						if(!offsetAutoCommit){
							lock.lock();
							String key = record.topic() + "_" + record.partition();
							OffsetCommitMeta meta;
							if(uncommittedOffsetMap.containsKey(key)){
								meta = uncommittedOffsetMap.get(key);
								meta.addOffset();
								System.out.println("-->key:" + key + ",offset:"+meta.getOffset());
							}
							lock.unlock();
						}
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
			consumer.close();
		}

	}
	
	private class OffsetCommitMeta{
		KafkaConsumer<String, Serializable> consumer;
		private final TopicPartition partition;
		private final AtomicLong offset;
		private final String key;
		private AtomicBoolean commitd = new AtomicBoolean(false);
		public OffsetCommitMeta(KafkaConsumer<String, Serializable> consumer,TopicPartition partition, long offset) {
			this.consumer = consumer;
			this.partition = partition;
			this.offset = new AtomicLong(offset);
			this.offset.incrementAndGet();
			this.key = partition.topic() + "_" + partition.partition();
		}
		
		public KafkaConsumer<String, Serializable> getConsumer() {
			return consumer;
		}

		public TopicPartition getPartition() {
			return partition;
		}

		public long getOffset() {
			return offset.get();
		}

		public long addOffset() {
			commitd.set(false);
			return offset.incrementAndGet();
		}
		public String getKey() {
			return key;
		}
		public boolean isCommitd() {
			return commitd.get();
		}
		public void setCommitd(boolean commitd) {
			this.commitd.set(commitd);
		}
		
	}

}
