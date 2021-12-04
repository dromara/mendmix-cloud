package com.jeesuite.amqp.qcloud.cmq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.amqp.MQContext;
import com.jeesuite.common.util.JsonUtils;
import com.jeesuite.common.util.ResourceUtils;
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

	private static Logger logger = LoggerFactory.getLogger("com.jeesuite.amqp");
	private Account account;
	
	private String queueName;
	private volatile Queue queue;
	private Map<String, Topic> topicMappings = new ConcurrentHashMap<String, Topic>();
	
	private static CMQManager instance = new CMQManager();

	private CMQManager() {
		doInit();
	}

	private void doInit() {
		queueName = MQContext.getGroupName();
		CmqConfig config = ResourceUtils.getBean("jeesuite.amqp.cmq.", CmqConfig.class);
	
		Validate.notBlank(config.getEndpoint(),"config[mq.cmq.endpoint] not found");
		Validate.notBlank(config.getSecretId(),"config[mq.cmq.secretId] not found");
		Validate.notBlank(config.getSecretKey(),"config[mq.cmq.secretKey] not found");

		config.setAlwaysPrintResultLog(false);
		config.setPrintSlow(false);
		this.account = new Account(config);		
		logger.info("init CMQ Account OK -> endpoint:{}",config.getEndpoint());
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
			logger.info(">>subscriptionName:{} for queue:{},topic:{}",subscriptionName,instance.queueName,topicName);
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
			QueueMeta meta = ResourceUtils.getBean("jeesuite.amqp.cmq.", QueueMeta.class);
			account.createQueue(queueName, meta);
			//
			logger.info("createQueue finished -> queueName:{}",queueName);
		} catch (Exception e) {
			if(!e.getMessage().contains("is already existed")){				
				throw new RuntimeException(e);
			}
			logger.info("queueName:{} is already existed",queueName);
		}
		return account.getQueue(queueName);
	}
	
	private static void createTopic(String topicName) {
		try {
			logger.info("createTopic begin -> topicName:",topicName);
			int maxMsgSize = 1024*1024;
			getAccount().createTopic(topicName, maxMsgSize);
			logger.info("createTopic finished -> topicName:{}",topicName);
		}catch (Exception e) {
			if(!e.getMessage().contains("is already existed")){				
				throw new RuntimeException(e);
			}
			logger.info("topicName:{} is already existed",topicName);
		}
	}
	
	private static void createSubscription(String topicName,String queueName,String subscriptionName){
		try {
			getAccount().createSubscribe(topicName, subscriptionName, queueName, "queue");
			logger.info("createSubscription finished -> subscriptionName:{}",subscriptionName);
		}catch (Exception e) {
			if(!e.getMessage().contains("is already existed")){				
				throw new RuntimeException(e);
			}
			logger.info("subscriptionName:{} is already existed",subscriptionName);
		}
	}
	
}
