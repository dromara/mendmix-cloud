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
package com.mendmix.amqp;

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
	
	private static MQProducer producer;
	
	private MQInstanceDelegate() {}

	public static void setProducer(MQProducer producer) {
		MQInstanceDelegate.producer = producer;
	}

	public static void send(MQMessage message){
		if(producer == null){
			System.err.println("MQProducer did not Initialization,Please check config[mq.provider] AND [mq.producer.enabled]");
			return;
		}
		message.setTopic(MQContext.rebuildWithNamespace(message.getTopic()));
		producer.sendMessage(message, false);
	}
	
    public static void asyncSend(MQMessage message){
    	if(producer == null){
    		System.err.println("MQProducer did not Initialization,Please check config[mq.provider] AND [mq.producer.enabled]");
    		return;
		}
    	producer.sendMessage(message, true);
	}
}
