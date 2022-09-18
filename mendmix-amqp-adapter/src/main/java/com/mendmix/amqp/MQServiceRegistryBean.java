/*
 * Copyright 2016-2020 www.mendmix.com.
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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import com.mendmix.amqp.adapter.kafka.KafkaConsumerAdapter;
import com.mendmix.amqp.adapter.kafka.KafkaProducerAdapter;
import com.mendmix.amqp.adapter.memoryqueue.EventbusProducerAdapter;
import com.mendmix.amqp.adapter.qcloud.cmq.CMQConsumerAdapter;
import com.mendmix.amqp.adapter.qcloud.cmq.CMQProducerAdapter;
import com.mendmix.amqp.adapter.rabbitmq.RabbitmqConsumerAdapter;
import com.mendmix.amqp.adapter.rabbitmq.RabbitmqProducerAdapter;
import com.mendmix.amqp.adapter.redis.RedisConsumerAdapter;
import com.mendmix.amqp.adapter.redis.RedisProducerAdapter;
import com.mendmix.amqp.adapter.rocketmq.RocketProducerAdapter;
import com.mendmix.amqp.adapter.rocketmq.RocketmqConsumerAdapter;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.spring.InstanceFactory;
import com.mendmix.spring.helper.SpringAopHelper;

public class MQServiceRegistryBean implements InitializingBean,DisposableBean,ApplicationContextAware,PriorityOrdered {

	protected  static final Logger logger = LoggerFactory.getLogger("com.mendmix.amqp");
	
	private ApplicationContext applicationContext;
	private MQConsumer consumer;
	private MQProducer producer;
	
	
	@Override
	public void afterPropertiesSet() throws Exception {
		
		String providerName = MQContext.getProviderName();
		//
		if(MQContext.isProducerEnabled()){			
			startProducer(providerName);
			MQInstanceDelegate.setProducer(producer);
		}
		//
		if(MQContext.isConsumerEnabled()){			
			startConsumer(providerName);
		}
	}
	
    private void startProducer(String providerName) throws Exception{
    	
    	if("kafka".equals(providerName)){
    		producer = new KafkaProducerAdapter();
		}else if("rocketmq".equals(providerName)){
			producer = new RocketProducerAdapter();
		}else if("rabbitmq".equals(providerName)){
			producer = new RabbitmqProducerAdapter();
		}else if("aliyun-mns".equals(providerName)){
			//TODO 
		}else if("aliyun-ons".equals(providerName)){
			//TODO 
		}else if("qcloud-cmq".equals(providerName)){
			producer = new CMQProducerAdapter();
		}else if("eventbus".equals(providerName)){
			producer = new EventbusProducerAdapter();
		}else if("redis".equals(providerName)){
			producer = new RedisProducerAdapter();
		}else{
			throw new MendmixBaseException("NOT_SUPPORT[providerName]:" + providerName);
		}
		producer.start();
		logger.info("MENDMIX-TRACE-LOGGGING-->> MQ_PRODUCER started -> groupName:{},providerName:{}",MQContext.getGroupName(),providerName);
	}
    
    private void startConsumer(String providerName) throws Exception{
		Map<String, MessageHandler> messageHanlders = applicationContext.getBeansOfType(MessageHandler.class);
		if(messageHanlders != null && !messageHanlders.isEmpty()){
			Map<String, MessageHandler> messageHandlerMaps = new HashMap<>(); 
			messageHanlders.values().forEach(e -> {
				Object origin = e;
				try {origin = SpringAopHelper.getTarget(e);} catch (Exception ex) {ex.printStackTrace();}
				MQTopicRef topicRef = origin.getClass().getAnnotation(MQTopicRef.class);
				String topicName = MQContext.rebuildWithNamespace(topicRef.value());
				messageHandlerMaps.put(topicName, e);
				logger.info("MENDMIX-TRACE-LOGGGING-->> ADD MQ_COMSUMER_HANDLER ->topic:{},handlerClass:{} ",topicName,e.getClass().getName());
			});
			
			if("kafka".equals(providerName)){
				consumer = new KafkaConsumerAdapter(messageHandlerMaps);
			}else if("rocketmq".equals(providerName)){
				consumer = new RocketmqConsumerAdapter(messageHandlerMaps);
			}else if("rabbitmq".equals(providerName)){
				consumer = new RabbitmqConsumerAdapter(messageHandlerMaps);
			}else if("aliyun-mns".equals(providerName)){
				//TODO 
			}else if("aliyun-ons".equals(providerName)){
				//TODO 
			}else if("qcloud-cmq".equals(providerName)){
				consumer = new CMQConsumerAdapter(messageHandlerMaps);
			}else if("eventbus".equals(providerName)){
				EventbusProducerAdapter.setMessageHandlers(messageHandlerMaps);
			}else if("redis".equals(providerName)){
				consumer = new RedisConsumerAdapter(messageHandlerMaps);
			}else{
				throw new MendmixBaseException("NOT_SUPPORT[providerName]:" + providerName);
			}
			
			if(consumer != null) {
				consumer.start();
			}
			logger.info("MENDMIX-TRACE-LOGGGING-->> MQ_COMSUMER started -> groupName:{},providerName:{}",MQContext.getGroupName(),providerName);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		InstanceFactory.setApplicationContext(applicationContext);
	}
	
	@Override
	public void destroy() throws Exception {
		if(consumer != null) {
			consumer.shutdown();
		}
		if(producer != null) {
			producer.shutdown();
		}
		MQContext.close();
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
