/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.amqp.adapter;

import org.dromara.mendmix.amqp.MQContext;
import org.dromara.mendmix.amqp.MQContext.ActionType;
import org.dromara.mendmix.amqp.MQMessage;
import org.dromara.mendmix.amqp.MQProducer;
import org.dromara.mendmix.amqp.MessageStateCheckService;
import org.dromara.mendmix.amqp.adapter.eventbus.EventbusProducerAdapter;
import org.dromara.mendmix.common.guid.GUID;
import org.dromara.mendmix.spring.InstanceFactory;

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
	
	public void prepareHandle(MQMessage message) {
		message.initContextHeaders();
	}
	
	public String sendTxMessage(MQMessage message) {
		message.setTxId(String.valueOf(GUID.guid()));
		String msgId = sendMessage(message, false);
		messageStateCheckService().saveMessageTx(message);
		return msgId;
	}

}
