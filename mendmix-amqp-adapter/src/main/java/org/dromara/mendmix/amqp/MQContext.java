/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.amqp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.async.StandardThreadExecutor;
import org.dromara.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import org.dromara.mendmix.common.util.ExceptionFormatUtils;
import org.dromara.mendmix.common.util.ResourceUtils;

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
	
	public static final String APPLICATION = "application";
	public static final String MQ_CONTEXT_IGNORE_LOGGING = "mq_ignore_logging";
	
	public static enum ActionType {
		pub,sub
	}
	
	private String instance;
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
	private static ThreadPoolExecutor logHandleExecutor;
	
	private Map<String, List<String>> consumeAllowFilters = new HashMap<>();
	private Map<String, List<String>> consumeIgnoreFilters = new HashMap<>();
	
	private Properties properties;

	public MQContext(String instance) {
		this.instance = instance;
		providerName = ResourceUtils.getAndValidateProperty(instance + ".amqp.provider");
		if("none".equals(providerName))return;
		producerEnabled = Boolean.parseBoolean(ResourceUtils.getProperty(instance + ".amqp.producer.enabled"));
		consumerEnabled = Boolean.parseBoolean(ResourceUtils.getProperty(instance + ".amqp.consumer.enabled"));
		//
		String namespace = ResourceUtils.getProperty(instance + ".amqp.namespace");
		if(StringUtils.isNotBlank(namespace) && !"none".equals(namespace)){
			this.namespacePrefix = namespace + "_";
		}
		//
		String groupName = ResourceUtils.getProperty(instance + ".amqp.groupName",GlobalContext.APPID);
		if(ResourceUtils.getBoolean(instance + ".amqp.consumer.parallel")) {
			groupName = groupName + "_" + GlobalContext.getWorkerId();
		}
		this.groupName = rebuildWithNamespace(groupName);
		//
		asyncConsumeEnabled = Boolean.parseBoolean(ResourceUtils.getProperty(instance + ".amqp.consumer.async.enabled", "true"));
		processThreads = ResourceUtils.getInt(instance + ".amqp.consumer.processThreads", 20);
		batchSize = ResourceUtils.getInt(instance + ".amqp.consumer.fetch.batchSize", 1);
		consumeMaxInterval = ResourceUtils.getLong(instance + ".amqp.consumer.maxInterval.ms",24 * 3600 * 1000);
		consumeMaxRetryTimes = ResourceUtils.getInt(instance + ".amqp.consumer.maxRetryTimes",10);
		this.loghandlerEnabled = ResourceUtils.getBoolean(instance + ".amqp.message.logging.enabled");	
		//
		if(this.loghandlerEnabled) {
			if(logHandleExecutor == null) {
				final StandardThreadFactory threadFactory = new StandardThreadFactory("logHandleExecutor");
				logHandleExecutor = new StandardThreadExecutor(1, 10,60, TimeUnit.SECONDS,5000,threadFactory,new DiscardPolicy());
			}
		}
		this.loghandlerEnabled = false;
		//
		if(!"eventbus".equals(getProviderName()) 
				&& ResourceUtils.containsProperty(instance + ".amqp.internalTopics")) {
			internalTopics = ResourceUtils.getList(instance + ".amqp.internalTopics");
		}
		//
		properties = parseProfileProperties();
		//
		initConsumeFilterRules();
	}

	private void initConsumeFilterRules() {
		//blacklist,whitelist
		//application.amqp.consumer.ignoreRules[x-tenant-id]=blacklist:111;222
		if(isConsumerEnabled()) {
			consumeIgnoreFilters.clear();
			consumeAllowFilters.clear();
			Map<String, String> mappingValues = ResourceUtils.getMappingValues(instance + ".amqp.consumer.ignoreRules");
			mappingValues.forEach((k,v) -> {
				String[] parts = StringUtils.split(v, ":");
				String[] values;
				if(parts.length == 1) {
					values = StringUtils.split(v, ",;");
				}else {
					values = StringUtils.split(parts[1], ",;");
				}
				if("blacklist".equals(parts[0])) {
					consumeIgnoreFilters.put(k, Arrays.asList(values));
				}else {
					consumeAllowFilters.put(k, Arrays.asList(values));
				}
			});
		}
	}

	public String getInstance() {
		return instance;
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
	
	
	public Properties getProfileProperties() {
		return properties;
	}

	public String getProfileProperties(String key) {
		return properties.getProperty(key);
	}
	
	private Properties parseProfileProperties() {
		//application.amqp.kafka[bootstrap.servers]=10.39.61.86:30015
		//application.amqp.kafka.bootstrap.servers=10.39.61.86:30015
		Properties properties = new Properties();
		String prefix = String.format("%s.amqp.%s", instance,providerName);
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
	
	public String getStateCheckUrl() {
		//TODO
		return null;
	}
	
	public boolean matchedOnFilter(MQMessage message) {
		if(consumeAllowFilters.isEmpty() && consumeIgnoreFilters.isEmpty()) {
			return true;
		}
		Map<String, String> headers = message.getHeaders();
		if(headers == null || headers.isEmpty()) {
			return true;
		}
		
		boolean matched = true;
		for (String headerName : headers.keySet()) {
			if(consumeAllowFilters.containsKey(headerName)) {
				matched = consumeAllowFilters.get(headerName).contains(headers.get(headerName));
			}else if(consumeIgnoreFilters.containsKey(headerName)) {
				matched = !consumeIgnoreFilters.get(headerName).contains(headers.get(headerName));
			}
			if(!matched)break;
		}
		return matched;
	}
	
	public void updateConfigs(Properties properties) {
		String prefix = instance + ".amqp.consumer.ignoreRules";
		if(properties.keySet().stream().anyMatch(o -> o.toString().startsWith(prefix))) {
			initConsumeFilterRules();
		}
	}

	public static void processMessageLog(MQContext context,MQMessage message,ActionType actionType,Throwable ex){
		if(!context.isLogEnabled() || ThreadLocalContext.exists(MQ_CONTEXT_IGNORE_LOGGING))return;
		if(context.ignoreLogTopics.contains(message.getTopic()))return;
		message.setProcessTime(System.currentTimeMillis());
		try {
			logHandleExecutor.execute(new Runnable() {
				@Override
				public void run() {}
			});
		} catch (Exception e) {
			System.err.println(ExceptionFormatUtils.buildExceptionMessages(e, 2));
		}
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
		builder.append("\n -instance:").append(instance);
		builder.append("\n -providerName:").append(providerName);
		builder.append("\n -groupName:").append(groupName);
		builder.append("\n -namespacePrefix:").append(namespacePrefix);
		builder.append("\n -producerEnabled:").append(isProducerEnabled());
		builder.append("\n -consumerEnabled:").append(isConsumerEnabled());
		if(!consumeAllowFilters.isEmpty()) {
			builder.append("\n -consumeWhitelistRules:").append(consumeAllowFilters);
		}
		if(!consumeIgnoreFilters.isEmpty()) {
			builder.append("\n -consumeBlacklistRules:").append(consumeIgnoreFilters);
		}
		return builder.toString();
	}
    
}
