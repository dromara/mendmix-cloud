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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.util.ResourceUtils;

/**
 * 
 * 
 * <br>
 * Class Name   : MQContext
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2017年9月23日
 */
public class MQContext {
	
	public static final String DEFAULT_INSTANCE_GROUP_NAME = "mendmix";
	
	public static enum ActionType {
		pub,sub
	}
	
	private String instanceGroup;
	private String providerName;
	private String groupName;
	private boolean producerEnabled;
	private boolean consumerEnabled;
	private String namespacePrefix;
	
	private List<String> internalTopics;
	
	private boolean asyncConsumeEnabled;
	
	private List<String>  ignoreLogTopics = new ArrayList<>();
	
	private int processThreads = 0;
	private int batchSize;
	private long consumeMaxInterval = -1;
	private int consumeMaxRetryTimes = -1;
	//
	private boolean loghandlerEnabled;
	private static MQLogHandler logHandler;
	private static ThreadPoolExecutor logHandleExecutor;
	
	private Properties properties;

	public MQContext(String instanceGroup) {
		this.instanceGroup = instanceGroup;
		providerName = ResourceUtils.getAndValidateProperty(instanceGroup + ".amqp.provider");
		if("none".equals(providerName))return;
		producerEnabled = Boolean.parseBoolean(ResourceUtils.getProperty(instanceGroup + ".amqp.producer.enabled"));
		consumerEnabled = Boolean.parseBoolean(ResourceUtils.getProperty(instanceGroup + ".amqp.consumer.enabled"));
		//
		String namespace = ResourceUtils.getProperty(instanceGroup + ".amqp.namespace");
		if(StringUtils.isNotBlank(namespace) && !"none".equals(namespace)){
			this.namespacePrefix = namespace + "_";
		}
		//
		String groupName = ResourceUtils.getProperty(instanceGroup + ".amqp.groupName",GlobalRuntimeContext.APPID);
		if(ResourceUtils.getBoolean(instanceGroup + ".amqp.consumer.parallel")) {
			int workId = GlobalRuntimeContext.getWorkId();
			groupName = groupName + "_" + workId;
		}
		this.groupName = rebuildWithNamespace(groupName);
		//
		asyncConsumeEnabled = Boolean.parseBoolean(ResourceUtils.getProperty(instanceGroup + ".amqp.consumer.async.enabled", "true"));
		processThreads = ResourceUtils.getInt(instanceGroup + ".amqp.consumer.processThreads", 20);
		batchSize = ResourceUtils.getInt(instanceGroup + ".amqp.consumer.fetch.batchSize", 1);
		consumeMaxInterval = ResourceUtils.getLong(instanceGroup + ".amqp.consume.maxInterval.ms",24 * 3600 * 1000);
		consumeMaxRetryTimes = ResourceUtils.getInt(instanceGroup + ".amqp.consume.maxRetryTimes",10);
		//this.loghandlerEnabled = 
		//
		if(!"eventbus".equals(getProviderName()) 
				&& ResourceUtils.containsProperty(instanceGroup + ".amqp.internalTopics")) {
			internalTopics = ResourceUtils.getList(instanceGroup + ".amqp.internalTopics");
		}

		//
		properties = parseProfileProperties();
	}

	public String getInstanceGroup() {
		return instanceGroup;
	}

	public String rebuildWithNamespace(String name){
    	if(this.namespacePrefix == null)return name;
    	if(name == null || name.startsWith(this.namespacePrefix))return name;
    	return this.namespacePrefix + name;
    }

	public String getProviderName() {
		return providerName;
	}
	
	public String getGroupName() {
		return this.groupName;
	}

	public boolean isProducerEnabled() {
		return producerEnabled;
	}
	
	public boolean isConsumerEnabled() {
		return consumerEnabled;
	}
	
	public String getProfileProperties(String key) {
		return properties.getProperty(key);
	}
	
	private Properties parseProfileProperties() {
		//application.amqp.kafka[bootstrap.servers]=10.39.61.86:30015
		//application.amqp.kafka.bootstrap.servers=10.39.61.86:30015
		Properties properties = new Properties();
		String prefix = String.format("%s.amqp.%s", instanceGroup,providerName);
		Set<Entry<Object, Object>> entrySet = ResourceUtils.getAllProperties(prefix).entrySet();
		String key;
		String profileKey;
		for (Entry<Object, Object> entry : entrySet) {
			key = entry.getKey().toString();
			if(key.contains("[")) {
				profileKey = StringUtils.split(entry.getKey().toString(), "[]")[1];
			}else {
				profileKey = key.substring(prefix.length() + 1);
			}
			properties.setProperty(profileKey, entry.getValue().toString());
		}
		return properties;
	}
	
	/**
	 * 是否异步处理消息
	 * @return
	 */
	public boolean isAsyncConsumeEnabled() {
		return this.asyncConsumeEnabled;
	}
	
	public boolean isLogEnabled() {
		return this.loghandlerEnabled;
	}

	public int getMaxProcessThreads() {
		return this.processThreads;
	}
	
	public long getConsumeMaxInterval() {
		return consumeMaxInterval;
	}

	public int getConsumeMaxRetryTimes() {
		return consumeMaxRetryTimes;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public static void processMessageLog(MQContext context,MQMessage message,ActionType actionType,Throwable ex){
		if(!context.isLogEnabled())return;
		if(context.ignoreLogTopics.contains(message.getTopic()))return;
		message.setProcessTime(System.currentTimeMillis());
		logHandleExecutor.execute(new Runnable() {
			@Override
			public void run() {
				if(ex == null) {
					logHandler.onSuccess(context.getGroupName(),actionType,message);
				}else {
					logHandler.onError(context.getGroupName(),actionType, message, ex);
				}
			}
		});
	}
	
	public boolean hasInternalTopics() {
		return this.internalTopics != null && !this.internalTopics.isEmpty();
	}
	
	public boolean isInternalTopicMode(String topic) {
		if(this.internalTopics == null)return false;
		return this.internalTopics.contains(topic);
	}
	
	
	public static void close() {
		if(logHandleExecutor != null) {
			logHandleExecutor.shutdown();
			logHandleExecutor = null;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("MQContext");
		builder.append("\n -instance:").append(instanceGroup);
		builder.append("\n -providerName:").append(providerName);
		builder.append("\n -groupName:").append(groupName);
		builder.append("\n -namespacePrefix:").append(namespacePrefix);
		builder.append("\n -producerEnabled:").append(isProducerEnabled());
		builder.append("\n -consumerEnabled:").append(isConsumerEnabled());
		return builder.toString();
	}
    
}
