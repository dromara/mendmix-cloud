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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import com.mendmix.amqp.adapter.eventbus.EventbusProducerAdapter;
import com.mendmix.amqp.adapter.kafka.KafkaConsumerAdapter;
import com.mendmix.amqp.adapter.kafka.KafkaProducerAdapter;
import com.mendmix.amqp.adapter.rabbitmq.RabbitmqConsumerAdapter;
import com.mendmix.amqp.adapter.rabbitmq.RabbitmqProducerAdapter;
import com.mendmix.amqp.adapter.redis.RedisConsumerAdapter;
import com.mendmix.amqp.adapter.redis.RedisProducerAdapter;
import com.mendmix.amqp.adapter.rocketmq.RocketProducerAdapter;
import com.mendmix.amqp.adapter.rocketmq.RocketmqConsumerAdapter;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.spring.InstanceFactory;
import com.mendmix.spring.helper.SpringAopHelper;


public class MQServiceRegistryBean implements InitializingBean, DisposableBean, ApplicationContextAware, PriorityOrdered {

    protected static final Logger logger = LoggerFactory.getLogger("com.mendmix.amqp");

    private ApplicationContext applicationContext;
    private Map<String, MQConsumer> consumers = new HashMap<>();
    private Map<String, MQProducer> producers = new HashMap<>();
    private Map<String, MQContext> contexts = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        
    	Enumeration<Object> keys = ResourceUtils.getAllProperties(".*\\.amqp.provider", false).keys();
        MQContext context;
        MQProducer producer;
        MQConsumer consumer;
    	while(keys.hasMoreElements()) {
    		context = new MQContext(keys.nextElement().toString().split("\\.")[0]);
    		if("none".equals(context.getProviderName()))continue;
    		logger.info(context.toString());
    		if(context.isProducerEnabled()) {
    			producer = startProducer(context);
    			if(producer != null)producers.put(context.getInstanceGroup(), producer);
    		}
    		if(context.isConsumerEnabled()) {
    			consumer = startConsumer(context);
    			if(consumer != null) {
    				consumers.put(context.getInstanceGroup(), consumer);
    			}
    		}
    		//
    		contexts.put(context.getInstanceGroup(), context);
        }
    	//
    	MQInstanceDelegate.init(contexts, producers);
    }

    private MQProducer startProducer(MQContext context) throws Exception {
    	String providerName = context.getProviderName();
    	MQProducer producer = null;
        if ("rocketmq".equals(providerName)) {
            producer = new RocketProducerAdapter(context);
        } else if ("kafka".equals(providerName)) {
            producer = new KafkaProducerAdapter(context);
        } else if ("rabbitmq".equals(providerName)) {
            producer = new RabbitmqProducerAdapter(context);
        } else if ("redis".equals(providerName)) {
            producer = new RedisProducerAdapter(context);
        } else if ("eventbus".equals(providerName)) {
        	producer = new EventbusProducerAdapter(context);
        } else {
            throw new MendmixBaseException("NOT_SUPPORT[providerName]:" + providerName);
        }
        if(producer != null)producer.start();
        
        return producer;
    }

    private MQConsumer startConsumer(MQContext context) throws Exception {
    	String providerName = context.getProviderName();
        Map<String, MessageHandler> messageHanlders = applicationContext.getBeansOfType(MessageHandler.class);
        if (messageHanlders == null || messageHanlders.isEmpty()) return null;

        Map<String, MessageHandler> messageHandlerMaps = new HashMap<>();
        for (MessageHandler handler : messageHanlders.values()) {
            Object origin = handler;
            try {
                origin = SpringAopHelper.getTarget(handler);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            MQTopicRef topicRef = origin.getClass().getAnnotation(MQTopicRef.class);
            if(topicRef != null && !StringUtils.equals(context.getInstanceGroup(), topicRef.instance())) {
            	continue;
            }
            String topicName = handler.topicName();
            if(StringUtils.isBlank(topicName) && topicRef != null) {
                topicName = context.rebuildWithNamespace(topicRef.value());
            }
            Validate.notBlank(topicName, "topic define is null for:" + origin.getClass().getSimpleName());
            messageHandlerMaps.put(topicName, handler);
            logger.info(">> ADD MQ_COMSUMER_HANDLER ->instance:{},topic:{},handlerClass:{} ", context.getInstanceGroup(),topicName, handler.getClass().getName());
        }
        
        if(messageHandlerMaps.isEmpty()) { 
        	return null;
        }

        MQConsumer consumer = null;
        if ("rocketmq".equals(providerName)) {
            consumer = new RocketmqConsumerAdapter(context,messageHandlerMaps);
        }  else if ("eventbus".equals(providerName)) {
        	EventbusProducerAdapter.setMessageHandlers(messageHandlerMaps);
        } else if ("rabbitmq".equals(providerName)) {
        	consumer = new RabbitmqConsumerAdapter(context,messageHandlerMaps);
        }  else if ("kafka".equals(providerName)) {
            consumer = new KafkaConsumerAdapter(context,messageHandlerMaps);
        } else if ("redis".equals(providerName)) {
        	consumer = new RedisConsumerAdapter(context, messageHandlerMaps);
        } else {
            throw new MendmixBaseException("NOT_SUPPORT[providerName]:" + providerName);
        }
        //
        if(context.hasInternalTopics()) {
        	EventbusProducerAdapter.setMessageHandlers(messageHandlerMaps);
        }

        if(consumer != null)consumer.start();
        logger.info(">> MQ_COMSUMER started -> groupName:{},providerName:{}", context.getGroupName(), providerName);
        
        return consumer;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        InstanceFactory.setApplicationContext(applicationContext);
    }

    @Override
    public void destroy() throws Exception {
    	for (MQProducer producer : producers.values()) {
    		producer.shutdown();
		}
        for (MQConsumer consumer : consumers.values()) {
        	consumer.shutdown();
		}
        MQContext.close();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
