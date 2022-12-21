/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.amqp.adapter.kafka;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.amqp.MQContext;
import com.mendmix.amqp.MQMessage;
import com.mendmix.amqp.MessageHandler;
import com.mendmix.amqp.adapter.AbstractConsumer;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.util.ResourceUtils;

/**
 * 
 * <br>
 * Class Name   : KafkaMQConsumer
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年9月18日
 */
public class KafkaConsumerAdapter extends AbstractConsumer {
	private final Logger logger = LoggerFactory.getLogger("com.mendmix.amqp.adapter");

	private KafkaConsumer<String, String> kafkaConsumer;
	
	private Duration timeoutDuration = Duration.ofMillis(ResourceUtils.getLong("mendmix.amqp.fetch.timeout.ms",100));
	
	private boolean offsetAutoCommit;
	
	private Map<TopicPartition, OffsetAndMetadataStat> uncommitOffsetStats = new ConcurrentHashMap<>();
	

	public KafkaConsumerAdapter(Map<String, MessageHandler> messageHandlers) {
		super(messageHandlers);
	}
	
	@Override
	public void start() throws Exception {
		logger.info("MENDMIX-TRACE-LOGGGING-->> KafkaConsumer start Begin..");
		Properties configs = buildConfigs();
		offsetAutoCommit = Boolean.parseBoolean(configs.getProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
		kafkaConsumer = new KafkaConsumer<>(configs);
		
		Set<String> topicNames = messageHandlers.keySet();
		if(offsetAutoCommit) {
			kafkaConsumer.subscribe(topicNames);
		}else {
			//
			ConsumerRebalanceListener listener = new ConsumerRebalanceListener() {
				//准备重新负载均衡，停止消费消息
				@Override
				public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
					//手动提交
					kafkaConsumer.commitSync();
				}
				//完成负载均衡，准备重新消费消息
				@Override
				public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
					uncommitOffsetStats.clear();
					for (TopicPartition partition : partitions) {
						uncommitOffsetStats.put(partition, new OffsetAndMetadataStat(0));
					}
				}
				
			};
			//
			kafkaConsumer.subscribe(topicNames,listener);
		}

		super.startWorker();
		
		logger.info("MENDMIX-TRACE-LOGGGING-->> KafkaConsumer start End -> subscribeTopics:{}",configs,topicNames);
	}
	
	@Override
	public List<MQMessage> fetchMessages() {
		 //手动提交offset
		trySubmitOffsets();

		 ConsumerRecords<String, String> records = kafkaConsumer.poll(timeoutDuration);
		 Iterator<ConsumerRecord<String, String>> iterator = records.iterator();
		 
		 List<MQMessage> result = new ArrayList<>(records.count());
		 MQMessage message;
		 ConsumerRecord<String, String> item;
		 while(iterator.hasNext()) {
			 item = iterator.next();
			 message = new MQMessage(item.topic(), item.value());
			 message.setOriginMessage(item);
			 result.add(message);
		 }
		 return result;
	}
	
	@Override
	public String handleMessageConsumed(MQMessage message) {
		if(offsetAutoCommit)return null;
		ConsumerRecord<String, String> originMessage = message.getOriginMessage(ConsumerRecord.class);
		
		TopicPartition partition = new TopicPartition(originMessage.topic(), originMessage.partition());
		//
		if(MQContext.isAsyncConsumeEnabled()){
			uncommitOffsetStats.get(partition).updateOnConsumed(originMessage.offset());
		}else {
			Map<TopicPartition, OffsetAndMetadata> uncommitOffsets = new HashMap<>(1);
			uncommitOffsets.put(partition, new OffsetAndMetadata(originMessage.offset() + 1));
			submitOffsets(uncommitOffsets);
		}

		return null;
	}
	
	private void trySubmitOffsets() {
		if(offsetAutoCommit || !MQContext.isAsyncConsumeEnabled()){
			return;
		}
		
		Map<TopicPartition, OffsetAndMetadata> uncommitOffsets = new HashMap<>(uncommitOffsetStats.size());
		uncommitOffsetStats.forEach( (k,v) -> {
			if(!v.isCommited()) {
				uncommitOffsets.put(k, new OffsetAndMetadata(v.getOffset() + 1));
			}
		} );
		
		submitOffsets(uncommitOffsets);
	}
	
	/**
	 * 手动提交offset
	 */
	private synchronized void submitOffsets(Map<TopicPartition, OffsetAndMetadata> uncommitOffsets) {
		if(uncommitOffsets.isEmpty())return;
		kafkaConsumer.commitAsync(uncommitOffsets, new OffsetCommitCallback() {
			@Override
			public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
				if(exception != null) {
					kafkaConsumer.commitSync(uncommitOffsets);
				}else {
					if(logger.isDebugEnabled())logger.debug("MENDMIX-TRACE-LOGGGING-->> MQmessage_COMMIT_SUCCESS -> offsets:{}",offsets);
				}
				//
				offsets.forEach( (k,v) -> {
					uncommitOffsetStats.get(k).setCommited(true);
				} );
			}
		});
	}
	
	private static Properties buildConfigs() {

		Properties result = new Properties();
		result.setProperty("group.id", MQContext.getGroupName());
		
		Class<ConsumerConfig> clazz = ConsumerConfig.class;
		Field[] fields = clazz.getDeclaredFields();
		String propName;
		String propValue;
		for (Field field : fields) {
			if (!field.getName().endsWith("CONFIG") || field.getType() != String.class) continue;
			field.setAccessible(true);
			try {
				propName = field.get(clazz).toString();
			} catch (Exception e) {
				continue;
			}
			propValue = ResourceUtils.getProperty("mendmix.amqp.kafka[" + propName + "]");
			if (StringUtils.isNotBlank(propValue)) {
				result.setProperty(propName, propValue);
			}
		}

		if (!result.containsKey(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)) {
        	throw new NullPointerException("Kafka config[bootstrap.servers] is required");
        }

		 if (!result.containsKey(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG)) {
	            result.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); // key serializer
	        }

	        if (!result.containsKey(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG)) {
	            result.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
	        }
	        
	        if (!result.containsKey(ConsumerConfig.CLIENT_ID_CONFIG)) {
	            result.put(ConsumerConfig.CLIENT_ID_CONFIG, MQContext.getGroupName() + GlobalRuntimeContext.getWorkId());
	        }
	        
	        //每批次最大拉取记录
	        if (!result.containsKey(ConsumerConfig.MAX_POLL_RECORDS_CONFIG)) {
	            result.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, MQContext.getMaxProcessThreads());
	        }
	        
	      //设置默认提交
			if (!result.containsKey(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG)) {
				result.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, MQContext.isAsyncConsumeEnabled() ? "false" : "true");
			}
			
			String kafkaSecurityProtocol = ResourceUtils.getProperty("mendmix.amqp.kafka[security.protocol]");
	        String kafkaSASLMechanism = ResourceUtils.getProperty("mendmix.amqp.kafka[sasl.mechanism]");
	        String config = ResourceUtils.getProperty("mendmix.amqp.kafka[sasl.jaas.config]");
	        if (!StringUtils.isEmpty(kafkaSecurityProtocol) && !StringUtils.isEmpty(kafkaSASLMechanism)
	                        && !StringUtils.isEmpty(config)) {
	                result.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, kafkaSecurityProtocol);
	                result.put(SaslConfigs.SASL_MECHANISM, kafkaSASLMechanism);
	                result.put("sasl.jaas.config", config);
	        }

			return result;
		}


		@Override
		public void shutdown() {
			super.shutdown();
			kafkaConsumer.close();
		}
		
}
