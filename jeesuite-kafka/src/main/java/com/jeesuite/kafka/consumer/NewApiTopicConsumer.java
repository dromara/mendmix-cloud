package com.jeesuite.kafka.consumer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

/**
 * 默认消费者实现（new consumer api）
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月12日
 */
public class NewApiTopicConsumer extends AbstractTopicConsumer implements TopicConsumer {

	private static final Logger logger = LoggerFactory.getLogger(ConsumerWorker.class);

	private Map<String, MessageHandler> topicHandlers;
	
	private List<ConsumerWorker> consumerWorks = new ArrayList<>();
	
	private boolean offsetAutoCommit;
	
	private Properties properties;
    private String clientIdPrefix;
    private long pollTimeout = 1000;
    
    //private ReentrantLock lock = new ReentrantLock();
    
    
	public NewApiTopicConsumer(ConsumerContext context) {
		super(context);
		properties = context.getProperties();
		clientIdPrefix = properties.getProperty(ConsumerConfig.CLIENT_ID_CONFIG);
		this.topicHandlers = context.getMessageHandlers();
		//enable.auto.commit 默认为true
		offsetAutoCommit = context.getProperties().containsKey("enable.auto.commit") == false || Boolean.parseBoolean(context.getProperties().getProperty("enable.auto.commit"));
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
			fetchExecutor.submit(worker);
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
//	                    if(startOffset == 0){
//	                    	OffsetAndMetadata offsetAndMetaData = worker.consumer.committed(tp);
//	                    	startOffset = offsetAndMetaData != null ? offsetAndMetaData.offset() : -1L;
//	                    }
						
						if (startOffset > 0){
							worker.consumer.seek(tp, startOffset);
							logger.info("topicPartion : {} seek offset : {}", tp, startOffset);
						}
						//
						//worker.addPartitions(tp, startOffset);
					}
					
					//提交分区
					//commitOffsets(worker);
				}
			};
			//
			worker.consumer.subscribe(topics, listener);
		}
	}
	
	
	
	private void commitOffsets(ConsumerWorker worker) {
		
		KafkaConsumer<String, Serializable> consumer = worker.consumer;
		if(worker.isCommiting())return;
		worker.setCommiting(true);
		try {

			if(worker.uncommittedOffsetMap.isEmpty())return ;
			
			logger.debug("committing the offsets : {}", worker.uncommittedOffsetMap);
			consumer.commitAsync(worker.uncommittedOffsetMap, new OffsetCommitCallback() {
				@Override
				public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
					//
					worker.setCommiting(false);
					if(exception == null){
						worker.resetUncommittedOffsetMap();
						logger.debug("committed the offsets : {}",offsets);
					}else{
						logger.error("committ the offsets error",exception);
					}
				}
			});
		} finally {
			
		}
	}
	
	@Override
	public void close() {
		if(!runing.get())return;
		//防止外部暂停了fetch发生阻塞
		consumerContext.switchFetch(true);
		for (int i = 0; i < consumerWorks.size(); i++) {
			consumerWorks.get(i).close();
			consumerWorks.remove(i);
			i--;
		}
		super.close();
	}
	
	private class ConsumerWorker implements Runnable {

		private AtomicBoolean closed = new AtomicBoolean();
		private AtomicBoolean commiting = new AtomicBoolean(false);//是否正在提交分区
		private AtomicInteger uncommittedNums = new AtomicInteger(0); //当前未提交记录
		KafkaConsumer<String, Serializable> consumer;
		private List<TopicPartition> assigndPartitions = new ArrayList<>();
		private Map<TopicPartition, OffsetAndMetadata> uncommittedOffsetMap = new ConcurrentHashMap<>();
		
		
		public ConsumerWorker(String topic,int index) {
			properties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, clientIdPrefix + "_" + index);
			consumer = new KafkaConsumer<String, Serializable>(properties);
		}
		
		public boolean isCommiting() {
			return commiting.get();
		}
		
		public void setCommiting(boolean commiting) {
			this.commiting.set(commiting);
		}

        public void addPartitions(TopicPartition partition,long offset){
        	assigndPartitions.add(partition);
        	uncommittedOffsetMap.put(partition, new OffsetAndMetadata(offset));
        	uncommittedNums.incrementAndGet();
        }
        
        public void resetUncommittedOffsetMap(){
        	uncommittedOffsetMap.clear();
        	uncommittedNums.set(0);
        }

		@Override
		public void run() {
			while (!closed.get()) {
				//提交分区
				if(uncommittedNums.get() > 0){					
					commitOffsets(this);
				}
				//如果拉取消息暂停
				while(!consumerContext.fetchEnabled()){
					try {Thread.sleep(1000);} catch (Exception e) {}
				}
				//当处理线程满后，阻塞处理线程
				while(true){
					if(defaultProcessExecutor.getMaximumPoolSize() > defaultProcessExecutor.getSubmittedTasksCount()){
						break;
					}
					try {Thread.sleep(100);} catch (Exception e) {}
				}
				
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
			   }
			}
			consumer.close();
			logger.info("consumer exited");
		}
		
		
		/**
		 * @param record
		 */
		private void processConsumerRecords(final ConsumerRecord<String, Serializable> record) {
			//兼容没有包装的情况
			final DefaultMessage message = record.value() instanceof DefaultMessage ? (DefaultMessage) record.value() : new DefaultMessage(record.key(),(Serializable) record.value());
			final MessageHandler messageHandler = topicHandlers.get(record.topic());
			
			message.setTopicMetadata(record.topic(), record.partition(), record.offset());
			
			consumerContext.updateConsumerStats(record.topic(),1);
			//
			consumerContext.saveOffsetsBeforeProcessed(record.topic(), record.partition(), record.offset());
			//第一阶段处理
			messageHandler.p1Process(message);
			//第二阶段处理
			(message.isConsumerAckRequired() ? highProcessExecutor : defaultProcessExecutor).submit(new Runnable() {
				@Override
				public void run() {
					try {									
						messageHandler.p2Process(message);
						//
						if(!offsetAutoCommit){
							uncommittedOffsetMap.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1));
							//
							uncommittedNums.incrementAndGet();
						}
						
						//回执
                        if(message.isConsumerAckRequired()){
                        	consumerContext.sendConsumerAck(message.getMsgId());
						}
						//
						consumerContext.saveOffsetsAfterProcessed(record.topic(), record.partition(), record.offset());
					} catch (Exception e) {
						boolean processed = messageHandler.onProcessError(message);
						if(processed == false){
							consumerContext.processErrorMessage(record.topic(), message);
						}
						logger.error("["+messageHandler.getClass().getSimpleName()+"] process Topic["+record.topic()+"] error",e);
					}
					
					consumerContext.updateConsumerStats(record.topic(),-1);
				}
			});
		}
		
		

		public void close() {
			closed.set(true);
			consumer.close();
		}

	}

}
