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
package com.mendmix.amqp.adapter.rocketmq;

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
	
	private final static Logger logger = LoggerFactory.getLogger("com.mendmix.amqp.adapter");
	
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
				logger.warn("MENDMIX-TRACE-LOGGGING-->> not messageHandler found for:{}",msg.getTopic());
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
			message.setHeaders(msg.getProperties());
			//上下文
			if(message.getHeaders() != null) {	
				CurrentRuntimeContext.addContextHeaders(message.getHeaders());
			}
			//消息状态检查
			if(!message.originStatusCompleted() && message.getConsumeTimes() <= 1) {
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			}
			try {
				messageHandlers.get(message.getTopic()).process(message);
				if(logger.isDebugEnabled())logger.debug("MENDMIX-TRACE-LOGGGING-->> MQ_MESSAGE_CONSUME_SUCCESS ->message:{}",message);
				//
				MQContext.processMessageLog(message, ActionType.sub,null);
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			} catch (Exception e) {
				logger.error(String.format("MENDMIX-TRACE-LOGGGING-->> MQ_MESSAGE_CONSUME_ERROR ->message:%s",message.toString()),e);
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
