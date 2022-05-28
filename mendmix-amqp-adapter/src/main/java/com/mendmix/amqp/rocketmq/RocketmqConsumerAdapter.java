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
package com.mendmix.amqp.rocketmq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.amqp.MQConsumer;
import com.mendmix.amqp.MQContext;
import com.mendmix.amqp.MQContext.ActionType;
import com.mendmix.amqp.MQMessage;
import com.mendmix.amqp.MessageHandler;
import com.mendmix.amqp.MessageHeaderNames;
import com.mendmix.amqp.MessageStatus;
import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.util.ResourceUtils;

/**
 * 
 * <br>
 * Class Name   : RocketmqConsumerAdapter
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年7月11日
 */
public class RocketmqConsumerAdapter implements MQConsumer {
	
	private final static Logger logger = LoggerFactory.getLogger("com.mendmix.amqp");
	
	private String namesrvAddr;
	
	private Map<String, MessageHandler> messageHandlers = new HashMap<>(); 
	
	private DefaultMQPushConsumer consumer;

	
	/**
	 * @param groupName
	 * @param namesrvAddr
	 * @param messageHandlers
	 */
	public RocketmqConsumerAdapter(Map<String, MessageHandler> messageHandlers) {
		this.namesrvAddr = ResourceUtils.getAndValidateProperty("mendmix.amqp.rocketmq.namesrvAddr");
		this.messageHandlers = messageHandlers;
	}


	/**
	 * @param namesrvAddr the namesrvAddr to set
	 */
	public void setNamesrvAddr(String namesrvAddr) {
		this.namesrvAddr = namesrvAddr;
	}

	@Override
	public void start() throws Exception {

		int consumeThreads = MQContext.getMaxProcessThreads();
		String groupName = MQContext.getGroupName();
		consumer = new DefaultMQPushConsumer(groupName);
		consumer.setNamesrvAddr(namesrvAddr);
		consumer.setConsumeMessageBatchMaxSize(1); //每次拉取一条
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setConsumeThreadMin(consumeThreads);
        consumer.setConsumeThreadMax(consumeThreads);
        consumer.setPullThresholdForQueue(1000);
        consumer.setConsumeConcurrentlyMaxSpan(500);
		for (String topic : messageHandlers.keySet()) {
			consumer.subscribe(topic, "*");
		}
		consumer.registerMessageListener(new customMessageListener());
		consumer.start();
	}


	
	private class customMessageListener implements MessageListenerConcurrently{
		@Override
		public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
			if(msgs.isEmpty())return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			MessageExt msg = msgs.get(0); //每次只拉取一条
			if(!messageHandlers.containsKey(msg.getTopic())) {
				logger.warn("not messageHandler found for:{}",msg.getTopic());
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
			if(MQContext.getConsumeMaxRetryTimes() > 0 && msg.getReconsumeTimes() > MQContext.getConsumeMaxRetryTimes()) {
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
			if(MQContext.getConsumeMaxInterval() > 0 && msg.getReconsumeTimes() > 1 && System.currentTimeMillis() - msg.getBornTimestamp() > MQContext.getConsumeMaxInterval()) {
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
			MQMessage message = new MQMessage(msg.getTopic(),msg.getTags(),msg.getKeys(), msg.getBody());
			message.setOriginMessage(msg);
			message.setRequestId(msg.getUserProperty(MessageHeaderNames.requestId.name()));
			message.setCheckUrl(msg.getUserProperty(MessageHeaderNames.checkUrl.name()));
			message.setProduceBy(msg.getUserProperty(MessageHeaderNames.produceBy.name()));
			message.setTenantId(msg.getUserProperty(MessageHeaderNames.tenantId.name()));
			message.setTransactionId(msg.getUserProperty(MessageHeaderNames.transactionId.name()));
			//多租户支持
			if(message.getTenantId() != null) {							
				CurrentRuntimeContext.setTenantId(message.getTenantId());
			}
			try {
				//事务消息检查
				if(message.getTransactionId() != null){
	            	String transactionStatus = message.checkTransactionStatus();
	            	if(transactionStatus != null) {
	            		if(transactionStatus.equals(MessageStatus.processed.name())) {
							logger.info("MQ_MESSAGE_TRANSACTION_STATUS_PROCESSED ->topic:{},requestId:{},transactionId:{}",message.getTopic(),message.getRequestId(),message.getTransactionId());
							return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
						}else if(transactionStatus.equals(MessageStatus.notExists.name())) {
							//考虑发起方事务提交可能延时等情况，这里开启一次重试
							if(msg.getReconsumeTimes() <= 1) {
								//
								MQContext.processMessageLog(message,ActionType.sub,new IllegalArgumentException("transactionId["+message.getTransactionId()+"] not found"));
								return ConsumeConcurrentlyStatus.RECONSUME_LATER;
							}else  {
								logger.info("MQ_MESSAGE_TRANSACTION_STATUS_INVALID ->topic:{},requestId:{},transactionId:{}",message.getTopic(),message.getRequestId(),message.getTransactionId());
								return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
							}
						}
	            	}
					if(logger.isDebugEnabled())logger.debug("MQ_MESSAGE_TRANSACTION_STATUS_VALID -> topic:{},transactionId:{}",message.getTopic(),message.getTransactionId());
				}
				
				messageHandlers.get(message.getTopic()).process(message);
				if(logger.isDebugEnabled())logger.debug("MQ_MESSAGE_CONSUME_SUCCESS ->message:{}",message);
				//
				MQContext.processMessageLog(message, ActionType.sub,null);
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			} catch (Exception e) {
				logger.error(String.format("MQ_MESSAGE_CONSUME_ERROR ->message:%s",message.toString()),e);
				//
				MQContext.processMessageLog(message,ActionType.sub, e);
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			}finally{
				ThreadLocalContext.unset();
			}				
		}
		
	}

	@Override
	public void shutdown() {
		consumer.shutdown();
	}
	
}
