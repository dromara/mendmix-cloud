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
package org.dromara.mendmix.amqp.adapter.rocketmq;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dromara.mendmix.amqp.MQContext;
import org.dromara.mendmix.amqp.MQMessage;
import org.dromara.mendmix.amqp.adapter.AbstractProducer;
import org.dromara.mendmix.common.util.ResourceUtils;

/**
 * 
 * <br>
 * Class Name   : RocketProducerAdapter
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年7月11日
 */
public class RocketProducerAdapter extends AbstractProducer {

	private final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.amqp.adapter");
	
	private String namesrvAddr;
	
	private DefaultMQProducer producer;
	
	/**
	 * @param groupName
	 * @param namesrvAddr
	 */
	public RocketProducerAdapter(MQContext context) {
		super(context);
		this.namesrvAddr = ResourceUtils.getAndValidateProperty(context.getInstance() + ".amqp.rocketmq[namesrvAddr]");		
	}

	@Override
	public void start() throws Exception{
		super.start();
		producer = new DefaultMQProducer(context.getGroupName());
		producer.setNamesrvAddr(namesrvAddr);
		producer.start();
	}
	
	//MessageQueueSelector
	@Override
	public String sendMessage(MQMessage message,boolean async) {
		prepareHandle(message);
		Message _message = new Message(message.getTopic(), message.getTag(), message.getBizKey(), message.bodyAsBytes());
		if(message.getHeaders() != null) {
			String headerValue;
			for (String name : message.getHeaders().keySet()) {
				headerValue = message.getHeaders().get(name);
				if(headerValue == null)continue;
				_message.putUserProperty(name, headerValue);
			}
		}
		try {
			if(async){
				producer.send(_message, new SendCallback() {
					@Override
					public void onSuccess(SendResult sendResult) {
						if(logger.isDebugEnabled())logger.debug("MQ_SEND_SUCCESS:{} -> msgId:{},status:{},offset:{}",message.getTopic(),sendResult.getMsgId(),sendResult.getSendStatus().name(),sendResult.getQueueOffset());
						message.onProducerFinished(sendResult.getMsgId(),0,sendResult.getQueueOffset());
						handleSuccess(message);
					}
					
					@Override
					public void onException(Throwable e) {
						handleError(message, e);
						logger.warn("MQ_SEND_FAIL:"+message.getTopic(),e);
					}
				});
			}else{
				SendResult sendResult = producer.send(_message);	
				message.onProducerFinished(sendResult.getMsgId(),0,sendResult.getQueueOffset());
				if(sendResult.getSendStatus() == SendStatus.SEND_OK) {
					handleSuccess(message);
				}else {
					handleError(message, new MQClientException(0, sendResult.getSendStatus().name()));
				}
			}
		} catch (Exception e) {
			handleError(message, e);
			throw new RuntimeException(e);
		}
		
		return null;
	}

	@Override
	public void shutdown() {
		super.shutdown();
		producer.shutdown();
	}

	

}
