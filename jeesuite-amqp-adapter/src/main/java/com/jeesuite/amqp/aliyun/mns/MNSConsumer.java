/*
 * Copyright 2016-2018 www.jeesuite.com.
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
package com.jeesuite.amqp.aliyun.mns;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.model.Message;
import com.jeesuite.common.async.StandardThreadExecutor;
import com.jeesuite.common.async.StandardThreadExecutor.StandardThreadFactory;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.spring.InstanceFactory;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2019年3月18日
 */
public class MNSConsumer implements InitializingBean,DisposableBean,PriorityOrdered{

	private static Logger logger = LoggerFactory.getLogger("com.aygframework.support");
	
	private Map<String, MNSQueueProcessHanlder> queueHanlders = new HashMap<>();
	
	private StandardThreadExecutor fetchExecutor;
	private StandardThreadExecutor defaultProcessExecutor;
	
	private AtomicBoolean closed = new AtomicBoolean(false);

	@Value("${aliyun.mns.consumer.queueName}")
	private String queueName;
	
	private Semaphore semaphore;

	public MNSConsumer() {}

	@Override
	public void afterPropertiesSet() throws Exception {
		start();
	}
	
	private void start(){
		CloudQueue queue = MNSClientInstance.createQueueIfAbsent(queueName);
		initTopicHanlders();
		fetchExecutor = new StandardThreadExecutor(1, 1,0, TimeUnit.SECONDS, 1,new StandardThreadFactory("mns-Fetch-Executor"));
		int maxThread = ResourceUtils.getInt("aliyun.mns.consumer.processThreads", 50);
		semaphore = new Semaphore(maxThread);
		defaultProcessExecutor = new StandardThreadExecutor(1, maxThread,60, TimeUnit.SECONDS, 1,new StandardThreadFactory("mns-defaultProcess-Executor"));
		fetchExecutor.submit(new Worker(queue));
		logger.info("start work for queue Ok -> queue:{}",queue.getQueueURL());
	}
	
	private void initTopicHanlders(){
		Map<String, MNSQueueProcessHanlder> interfaces = InstanceFactory.getInstanceProvider().getInterfaces(MNSQueueProcessHanlder.class);
		if(interfaces == null || interfaces.isEmpty())return; 
		for (MNSQueueProcessHanlder hanlder : interfaces.values()) {
			for (String topicName : hanlder.topicNames()) {	
				if(queueHanlders.containsKey(topicName)){
					throw new RuntimeException("ProcessHanlder for topicName ["+topicName+"] existed");
				}
				//
				MNSClientInstance.createTopicIfAbsent(topicName, queueName);
				
				queueHanlders.put(topicName, hanlder);
				logger.info("registered MNSHanlder Ok -> queue:{},topic:{},hander:{}",queueName,topicName,hanlder.getClass().getName());
			}
		}
		if(queueHanlders.isEmpty())throw new RuntimeException("not any MNS TopicHanlder found");
	}
	
	
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
	
	@Override
	public void destroy() throws Exception {
		closed.set(true);
		fetchExecutor.shutdown();
		defaultProcessExecutor.shutdown();
	}
	
	private class Worker implements Runnable{
       
		CloudQueue queue;
		public Worker(CloudQueue queue) {
			this.queue = queue;
		}

		@Override
		public void run() {
			while(!closed.get()){
				try {
					Message message = queue.popMessage(5);
					if(message != null){
						String messageBody = message.getMessageBodyAsRawString();
						JSONObject json = JSON.parseObject(messageBody);
						final String topicName = json.getString("topic");
						final String bodyString = json.getString("body");
						MNSQueueProcessHanlder hanlder = queueHanlders.get(topicName);
						if(hanlder == null)continue;
						//信号量获取通行证
						semaphore.acquire();
						
						defaultProcessExecutor.submit(new Runnable() {
							@Override
							public void run() {
								try {									
									logger.debug("processs_topic begin -> topicName:{},messageId:{}",topicName,message.getMessageId());
									hanlder.process(topicName,bodyString);
									queue.deleteMessage(message.getReceiptHandle());
									logger.debug("processs_topic end -> topicName:{},messageId:{},DequeueCount:{}",topicName,message.getMessageId(),message.getDequeueCount());
								} finally {
									//释放信号量
									semaphore.release();
								}
							}
						});
					}
				} catch (Exception e) {
					logger.error("mns_unknow_error",e);
				}
				
			}
		}
		
	}


}
