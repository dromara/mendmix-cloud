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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.amqp.MQContext;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.ResourceUtils;
import com.qcloud.cmq.Account;
import com.qcloud.cmq.Queue;
import com.qcloud.cmq.QueueMeta;
import com.qcloud.cmq.Subscription;
import com.qcloud.cmq.Topic;
import com.qcloud.cmq.entity.CmqConfig;

/**
 * 
 * <br>
 * Class Name   : CMQManager
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年10月28日
 */
public class CMQManager {

	private static Logger logger = LoggerFactory.getLogger("com.mendmix.amqp.adapter");
	private Account account;
	
	private String queueName;
	private volatile Queue queue;
	private Map<String, Topic> topicMappings = new ConcurrentHashMap<String, Topic>();
	
	private static CMQManager instance = new CMQManager();

	private CMQManager() {}

	public static void doInit(MQContext context) {
		if(instance.account != null)return;
		instance.queueName = context.getGroupName();
		CmqConfig config = ResourceUtils.getBean("mendmix.amqp.cmq.", CmqConfig.class);
	
		Validate.notBlank(config.getEndpoint(),"config[mq.cmq.endpoint] not found");
		Validate.notBlank(config.getSecretId(),"config[mq.cmq.secretId] not found");
		Validate.notBlank(config.getSecretKey(),"config[mq.cmq.secretKey] not found");

		config.setAlwaysPrintResultLog(false);
		config.setPrintSlow(false);
		instance.account = new Account(config);		
		logger.info("MENDMIX-TRACE-LOGGGING-->> init CMQ Account OK -> endpoint:{}",config.getEndpoint());
	}
	

	public static Account getAccount() {
		return instance.account;
	}
	
	public static Queue getQueue() {
		if(instance.queue != null) {
			return instance.queue;
		}
		synchronized (instance) {
			if(instance.queue != null) {
				return instance.queue;
			}
			instance.queue = instance.createQueueIfAbsent();
		}
		return instance.queue;
	}

	private Queue createQueueIfAbsent(){
		Queue queue = account.getQueue(queueName);
		try {
			List<String> existList = new ArrayList<>(1);
			account.listQueue(queueName, -1, -1, existList);
			if(!existList.contains(queueName)) {
				queue = createQueue(queueName);
			}
			QueueMeta meta = queue.getQueueAttributes();
			System.out.println(">>QueueMeta:" + JsonUtils.toJson(meta));
		}catch (Exception e) {
			throw new RuntimeException(e);
		} 
		return queue;
	}
	
	public static Topic createTopicIfAbsent(String topicName){
		if(instance.topicMappings.containsKey(topicName)){
			return instance.topicMappings.get(topicName);
		}
		Topic topic = getAccount().getTopic(topicName);
		
		try {
			List<String> existList = new ArrayList<>(1);
			getAccount().listTopic(topicName, existList, -1, -1);
			if (!existList.contains(topicName)) {
				createTopic(topicName);
			}
		}catch (Exception e) {
			throw new RuntimeException(e);
		} 
		instance.topicMappings.put(topicName, topic);
		return topic;
	}
	
	public static Subscription createSubscriptionIfAbsent(final String topicName){
		if(instance.queue == null) {
			getQueue();
		}
		Topic topic = CMQManager.createTopicIfAbsent(topicName);
		String subscriptionName = buildSubscriptionName(topicName,instance.queueName);
		Subscription subscription = getAccount().getSubscription(topicName, subscriptionName);
		try {
			List<String> existList = new ArrayList<>(1);
			topic.ListSubscription(-1, -1, subscriptionName, existList);
			if(!existList.contains(subscriptionName)) {
				createSubscription(topicName, instance.queueName, subscriptionName);
			}
			logger.info("MENDMIX-TRACE-LOGGGING-->> subscriptionName:{} for queue:{},topic:{}",subscriptionName,instance.queueName,topicName);
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return subscription;
	}
	
	
	/**
	 * @param topicName
	 * @param queueName
	 * @return
	 */
	private static String buildSubscriptionName(String topicName, String queueName) {
		return String.format("sub-for_%s-%s", queueName,topicName);
	}

	private Queue createQueue(String queueName) {
		try {
			QueueMeta meta = ResourceUtils.getBean("mendmix.amqp.cmq.", QueueMeta.class);
			account.createQueue(queueName, meta);
			//
			logger.info("MENDMIX-TRACE-LOGGGING-->> createQueue finished -> queueName:{}",queueName);
		} catch (Exception e) {
			if(!e.getMessage().contains("is already existed")){				
				throw new RuntimeException(e);
			}
			logger.info("MENDMIX-TRACE-LOGGGING-->> queueName:{} is already existed",queueName);
		}
		return account.getQueue(queueName);
	}
	
	private static void createTopic(String topicName) {
		try {
			logger.info("MENDMIX-TRACE-LOGGGING-->> createTopic begin -> topicName:",topicName);
			int maxMsgSize = 1024*1024;
			getAccount().createTopic(topicName, maxMsgSize);
			logger.info("MENDMIX-TRACE-LOGGGING-->> createTopic finished -> topicName:{}",topicName);
		}catch (Exception e) {
			if(!e.getMessage().contains("is already existed")){				
				throw new RuntimeException(e);
			}
			logger.info("MENDMIX-TRACE-LOGGGING-->> topicName:{} is already existed",topicName);
		}
	}
	
	private static void createSubscription(String topicName,String queueName,String subscriptionName){
		try {
			getAccount().createSubscribe(topicName, subscriptionName, queueName, "queue");
			logger.info("MENDMIX-TRACE-LOGGGING-->> createSubscription finished -> subscriptionName:{}",subscriptionName);
		}catch (Exception e) {
			if(!e.getMessage().contains("is already existed")){				
				throw new RuntimeException(e);
			}
			logger.info("MENDMIX-TRACE-LOGGGING-->> subscriptionName:{} is already existed",subscriptionName);
		}
	}
	
}
