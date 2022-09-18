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
package com.mendmix.amqp.adapter.qcloud.cmq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.mendmix.amqp.MQMessage;
import com.mendmix.amqp.MessageHandler;
import com.mendmix.amqp.adapter.AbstractConsumer;
import com.qcloud.cmq.Message;

/**
 * 
 * <br>
 * Class Name   : CMQConsumer
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年10月28日
 */
public class CMQConsumerAdapter extends AbstractConsumer {

	public CMQConsumerAdapter(Map<String, MessageHandler> messageHandlers) {
		super(messageHandlers);
	}

	@Override
	public void start() throws Exception {
		Set<String> topicNames = messageHandlers.keySet();
		for (String topic : topicNames) {
			CMQManager.createSubscriptionIfAbsent(topic);
		}
		//
		super.startWorker();
	}

	@Override
	public List<MQMessage> fetchMessages() {
		try {
			List<Message> messages;
			try {
				messages = CMQManager.getQueue().batchReceiveMessage(batchSize);
			} catch (com.qcloud.cmq.CMQServerException e) {
				//(10200)no message
				if(e.getMessage().equals("(10200)no message")) {
					return null;					
				}
				throw e;
			}
			if(messages == null || messages.isEmpty()) {
				return new ArrayList<>(0);
			}
			return messages.stream().map(o -> {
				MQMessage message = MQMessage.build(o.msgBody);
				message.setOriginMessage(o);
				return message;
			}).collect(Collectors.toList());
		} catch (Exception e) {
			logger.error("MENDMIX-TRACE-LOGGGING-->> ",e);
			return new ArrayList<>(0);
		}
	}

	@Override
	public String handleMessageConsumed(MQMessage message) {
		try {
			Message originMessage = message.getOriginMessage(Message.class);
			String receiptHandle = originMessage.receiptHandle;
			CMQManager.getQueue().deleteMessage(receiptHandle);
		} catch (Exception e) {
			logger.error("MENDMIX-TRACE-LOGGGING-->> ",e);
		}
		return null;
	}
	
	
	
}
