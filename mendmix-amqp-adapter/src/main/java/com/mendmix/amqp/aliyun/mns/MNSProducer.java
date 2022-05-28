/*
 * Copyright 2016-2018 www.mendmix.com.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aliyun.mns.client.CloudTopic;
import com.aliyun.mns.model.RawTopicMessage;
import com.aliyun.mns.model.TopicMessage;
import com.mendmix.amqp.MQMessage;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2019年3月18日
 */
public class MNSProducer {

	private Map<String, CloudTopic> topics = new ConcurrentHashMap<>();
	
	public MNSProducer() {}
	
	public String publishMessage(String topicName,Object data){
		CloudTopic topic = getTopic(topicName);
		TopicMessage tMessage = new RawTopicMessage();
		tMessage.setBaseMessageBody(new MQMessage(topicName, data).toMessageValue(true));
		topic.publishMessage(tMessage);
		
		return tMessage.getMessageId();
	}

	public CloudTopic getTopic(String topicName) {
		if(!topics.containsKey(topicName)){
			synchronized (this) {
				if(!topics.containsKey(topicName)){					
					CloudTopic topic = MNSClientInstance.createTopicIfAbsent(topicName, null);
					topics.put(topicName, topic);
				}
			}
		}
		return topics.get(topicName);
	}
}
