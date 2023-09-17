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
package com.mendmix.amqp;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.amqp.adapter.AbstractProducer;

/**
 * 
 * <br>
 * Class Name   : MQInstanceDelegate
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年7月11日
 */
public class MQInstanceDelegate {
	
	private static final Logger logger = LoggerFactory.getLogger("com.mendmix.amqp");

	private static Map<String, MQContext> contexts;
	private static Map<String, MQProducer> producers;
	
	private MQInstanceDelegate() {}

	public static void init(Map<String, MQContext> contexts,Map<String, MQProducer> producers) {
		MQInstanceDelegate.contexts = contexts;
		MQInstanceDelegate.producers = producers;
	}
	
	public static void send(String instance,MQMessage message){
		if(!producers.containsKey(instance)) {
			logger.warn(">>MQProducer not Initialization for:{}",instance);
			return;
		}
		sendMessage(contexts.get(instance), producers.get(instance), message, false);
	}
	
    public static void asyncSend(String instance,MQMessage message){
		if(!producers.containsKey(instance)) {
			logger.warn(">>MQProducer not Initialization for:{}",instance);
			return;
		}
		sendMessage(contexts.get(instance), producers.get(instance), message, true);
	}
    
    public static void sendTxMessage(String instance,MQMessage message){
		if(!producers.containsKey(instance)) {
			logger.warn(">>MQProducer not Initialization for:{}",instance);
			return;
		}
		sendTxMessage(contexts.get(instance), producers.get(instance), message);
	}
	
	public static void send(MQMessage message){
		send(MQContext.DEFAULT_INSTANCE_GROUP_NAME,message);
	}
	
	public static void asyncSend(MQMessage message){
		asyncSend(MQContext.DEFAULT_INSTANCE_GROUP_NAME,message);
	}

	public static void sendTxMessage(MQMessage message){
		sendTxMessage(MQContext.DEFAULT_INSTANCE_GROUP_NAME,message);
	}
	
	private static void sendMessage(MQContext context,MQProducer producer,MQMessage message,boolean async){
    	message.setTopic(context.rebuildWithNamespace(message.getTopic()));
    	if(context.isInternalTopicMode(message.getTopic())) {
    		AbstractProducer.getInternalMQProducer().sendMessage(message, async);
		}else {
			producer.sendMessage(message, async);
		}
	}
    
    private static void sendTxMessage(MQContext context,MQProducer producer,MQMessage message){
		message.setTopic(context.rebuildWithNamespace(message.getTopic()));
		if(context.isInternalTopicMode(message.getTopic())) {
			AbstractProducer.getInternalMQProducer().sendMessage(message,false);
		}else {
			producer.sendTxMessage(message);
		}
	}
}
