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
package com.mendmix.amqp.aliyun.mns;

import org.apache.commons.lang3.StringUtils;

import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.CloudTopic;
import com.aliyun.mns.client.MNSClient;
import com.aliyun.mns.common.ServiceException;
import com.aliyun.mns.model.PagingListResult;
import com.aliyun.mns.model.QueueMeta;
import com.aliyun.mns.model.SubscriptionMeta;
import com.aliyun.mns.model.TopicMeta;
import com.mendmix.common.util.ResourceUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2019年3月18日
 */
public class MNSClientInstance {

	private static MNSClient client;

	public static MNSClient getClient() {
		if(client != null)return client;
		synchronized (MNSClientInstance.class) {
			if(client != null)return client;
			String accessKeyId = ResourceUtils.getAndValidateProperty("aliyun.mns.accessKeyId");
			String accessKeySecret = ResourceUtils.getAndValidateProperty("aliyun.mns.accessKeySecret");
			String endpoint = ResourceUtils.getAndValidateProperty("aliyun.mns.endpoint");
			CloudAccount account = new CloudAccount(accessKeyId, accessKeySecret, endpoint);
			client = account.getMNSClient();
		}
		return client;
	}
	
	public static CloudQueue createQueueIfAbsent(String queueName){
		QueueMeta queueMeta = new QueueMeta();
		queueMeta.setQueueName(queueName);
		CloudQueue queue = getClient().getQueueRef(queueName);
		if(!queue.isQueueExist()){
			queue.create(queueMeta);
		}
		return queue;
	}
	
	public static CloudTopic createTopicIfAbsent(String topicName,String subForQueue){
		TopicMeta topicMeta = new TopicMeta();
		topicMeta.setTopicName(topicName);
		CloudTopic topic = getClient().getTopicRef(topicName);
		try {			
			topic.getAttribute();
		} catch (ServiceException e) {
			if("TopicNotExist".equals(e.getErrorCode())){				
				topic.create(topicMeta);
			}
		}	
		
		if(StringUtils.isNotBlank(subForQueue)){
			String subscriptionName = "sub-for-queue-"+subForQueue;
			PagingListResult<SubscriptionMeta> topicSubscriptions = topic.listSubscriptions(subscriptionName, "", 1);
			if(topicSubscriptions == null || topicSubscriptions.getResult() == null || topicSubscriptions.getResult().isEmpty()){
				//创建订阅关系
				SubscriptionMeta subMeta = new SubscriptionMeta();
				subMeta.setSubscriptionName(subscriptionName);
				subMeta.setEndpoint(topic.generateQueueEndpoint(subForQueue));
				subMeta.setNotifyContentFormat(SubscriptionMeta.NotifyContentFormat.SIMPLIFIED);
				subMeta.setNotifyStrategy(SubscriptionMeta.NotifyStrategy.EXPONENTIAL_DECAY_RETRY);
				
				topic.subscribe(subMeta);
			}
		}
		return topic;
	}

}
