/**
 * 
 */
package com.jeesuite.kafka.consumer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.kafka.handler.MessageHandler;
import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.monitor.ZkConsumerCommand;
import com.jeesuite.kafka.monitor.model.TopicPartitionInfo;
import com.jeesuite.kafka.serializer.MessageDecoder;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年7月1日
 */
@SuppressWarnings("deprecation")
public class OldApiTopicConsumer extends AbstractTopicConsumer implements TopicConsumer {

	private final static Logger logger = LoggerFactory.getLogger(OldApiTopicConsumer.class);
	
	private ConsumerConnector connector;
	private Deserializer<Object> deserializer;

	@SuppressWarnings("unchecked")
	public OldApiTopicConsumer(ConsumerContext context) {
		super(context);
		try {
			Class<?> deserializerClass = Class.forName(context.getProperties().getProperty("value.deserializer"));
			deserializer = (Deserializer<Object>) deserializerClass.newInstance();
		} catch (Exception e) {}
		this.connector = kafka.consumer.Consumer.createJavaConsumerConnector(new ConsumerConfig(context.getProperties()));
	}


	@Override
	public void start() {
		//重置offset
		if(consumerContext.getOffsetLogHanlder() != null){	
			resetCorrectOffsets();
		}
		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		for (String topicName : consumerContext.getMessageHandlers().keySet()) {
			int nThreads = 1;
			topicCountMap.put(topicName, nThreads);
			logger.info("topic[{}] assign fetch Threads {}",topicName,nThreads);
		}
		
		StringDecoder keyDecoder = new StringDecoder(new VerifiableProperties());
		MessageDecoder valueDecoder = new MessageDecoder(deserializer);

		Map<String, List<KafkaStream<String, Object>>> consumerMap = this.connector.createMessageStreams(topicCountMap,
				keyDecoder, valueDecoder);

		for (String topicName : consumerContext.getMessageHandlers().keySet()) {
			final List<KafkaStream<String, Object>> streams = consumerMap.get(topicName);

			for (final KafkaStream<String, Object> stream : streams) {
				MessageProcessor processer = new MessageProcessor(topicName, stream);
				this.fetchExecutor.execute(processer);
			}
		}
		//
		runing.set(true);
	}
	
	
	/**
	 * 按上次记录重置offsets
	 */
	private void resetCorrectOffsets() {
		String kafkaServers = consumerContext.getProperties().getProperty("bootstrap.servers");
		String zkServers = consumerContext.getProperties().getProperty("zookeeper.connect");
		if(StringUtils.isAnyBlank(kafkaServers,zkServers)){
			logger.warn("resetCorrectOffsets exit。Please check [bootstrap.servers] and [zookeeper.connect] is existing");
			return;
		}
		ZkConsumerCommand command = new ZkConsumerCommand(zkServers, kafkaServers);
		try {
			List<String> topics = command.getSubscribeTopics(consumerContext.getGroupId());
			for (String topic : topics) {
				List<TopicPartitionInfo> partitions = command.getTopicOffsets(consumerContext.getGroupId(), topic);
				for (TopicPartitionInfo partition : partitions) {
					//期望的偏移
					long expectOffsets = consumerContext.getLatestProcessedOffsets(topic, partition.getPartition());
					//
					if(expectOffsets > 0 && expectOffsets < partition.getOffset()){			
						command.resetTopicOffsets(consumerContext.getGroupId(), topic, partition.getPartition(), expectOffsets);
						logger.info("seek Topic[{}] partition[{}] from {} to {}",topic,partition.getPartition(),partition.getOffset(),expectOffsets);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		command.close();
	}

	/**
	 * 消息处理器
	 */
	class MessageProcessor implements Runnable {

		KafkaStream<String, Object> stream;

		private String topicName;
		
		private MessageHandler messageHandler;
		
		private String processorName;
		public MessageProcessor(String topicName, KafkaStream<String, Object> stream) {
			this.stream = stream;
			this.topicName = topicName;
			this.messageHandler = consumerContext.getMessageHandlers().get(topicName);
			this.processorName = this.messageHandler.getClass().getName();
		}

		@Override
		public void run() {
 
			logger.info("MessageProcessor [{}] start, topic:{}",Thread.currentThread().getName(),topicName);

			ConsumerIterator<String, Object> it = stream.iterator();
			// 没有消息的话，这里会阻塞
			while (it.hasNext()) {
				try {					
					MessageAndMetadata<String, Object> messageAndMeta = it.next();
					Object _message = messageAndMeta.message();
					DefaultMessage message = null;
					try {
						message = (DefaultMessage) _message;
					} catch (ClassCastException e) {
						message = new DefaultMessage(messageAndMeta.key(),(Serializable) _message);
					}
					message.setTopicMetadata(messageAndMeta.topic(), messageAndMeta.partition(), messageAndMeta.offset());
					consumerContext.updateConsumerStats(messageAndMeta.topic(),1);
					//
					consumerContext.saveOffsetsBeforeProcessed(messageAndMeta.topic(), messageAndMeta.partition(), messageAndMeta.offset() + 1);
					//第一阶段处理
					messageHandler.p1Process(message);
					//第二阶段处理
					submitMessageToProcess(topicName,messageAndMeta,message);
				} catch (Exception e) {
					logger.error("received_topic_error,topic:"+topicName,e);
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
				
			}
		
		}
		
		/**
		 * 提交消息到处理线程队列
		 * @param message
		 */
		private void submitMessageToProcess(final String topicName,final MessageAndMetadata<String, Object> messageAndMeta,final DefaultMessage message) {
			
			(message.isConsumerAckRequired() ? highProcessExecutor : defaultProcessExecutor).submit(new Runnable() {
				@Override
				public void run() {
					try {	
						long start = logger.isDebugEnabled() ? System.currentTimeMillis() : 0;
						messageHandler.p2Process(message);
						if(logger.isDebugEnabled()){
							long useTime = System.currentTimeMillis() - start;
							if(useTime > 1000)logger.debug("received_topic_useTime [{}]process topic:{} use time {} ms",processorName,topicName,useTime);
						}
						//回执
                        if(message.isConsumerAckRequired()){
                        	consumerContext.sendConsumerAck(message.getMsgId());
						}
						consumerContext.saveOffsetsAfterProcessed(messageAndMeta.topic(), messageAndMeta.partition(), messageAndMeta.offset() + 1);
					} catch (Exception e) {
						boolean processed = messageHandler.onProcessError(message);
						if(processed == false){
							consumerContext.processErrorMessage(topicName, message);
						}
						logger.error("received_topic_process_error ["+processorName+"]processMessage error,topic:"+topicName,e);
					}
					
					consumerContext.updateConsumerStats(messageAndMeta.topic(),-1);
				
				}
			});
		}
		
	}

	@Override
	public void close() {
		if(!runing.get())return;
		this.connector.commitOffsets();
		this.connector.shutdown();
		//防止外部暂停了fetch发生阻塞
		consumerContext.switchFetch(true);
		super.close();
		logger.info("KafkaTopicSubscriber shutdown ok...");
	}
	
}
