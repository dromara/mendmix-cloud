/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.mendmix.amqp.adapter;

import com.mendmix.amqp.MQContext;
import com.mendmix.amqp.MQContext.ActionType;
import com.mendmix.amqp.MQMessage;
import com.mendmix.amqp.MQProducer;
import com.mendmix.amqp.MessageStateCheckService;
import com.mendmix.amqp.adapter.eventbus.EventbusProducerAdapter;
import com.mendmix.common.guid.GUID;
import com.mendmix.spring.InstanceFactory;

public abstract class AbstractProducer implements MQProducer {
	
	protected MQContext context;
	
	private MessageStateCheckService messageStateCheckService;
	private static MQProducer internalMQProducer;
	
	public AbstractProducer(MQContext context) {
		this.context = context;
	}

	@Override
	public void start() throws Exception {
		messageStateCheckService = InstanceFactory.getInstance(MessageStateCheckService.class);
		if(context.hasInternalTopics()) {
			internalMQProducer = new EventbusProducerAdapter(context);
			internalMQProducer.start();
		}
	}


	public MessageStateCheckService messageStateCheckService() {
		if(messageStateCheckService != null)return messageStateCheckService;
		messageStateCheckService = InstanceFactory.getInstance(MessageStateCheckService.class);
		return messageStateCheckService;
	}


	public static MQProducer getInternalMQProducer() {
		return internalMQProducer;
	}


	@Override
	public void shutdown() {
		if(internalMQProducer != null)internalMQProducer.shutdown();
	}
	
	
	public void handleSuccess(MQMessage message) {
		MQContext.processMessageLog(context,message,ActionType.pub, null);
	}

	public void handleError(MQMessage message, Throwable e) {
		MQContext.processMessageLog(context,message,ActionType.pub, e);
	}
	
	
	public String sendTxMessage(MQMessage message) {
		message.setTxId(String.valueOf(GUID.guid()));
		String msgId = sendMessage(message, false);
		messageStateCheckService().saveMessageTx(message);
		return msgId;
	}

}
