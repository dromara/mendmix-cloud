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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.spring.InstanceFactory;

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
	
	public static enum ActionType {
		pub,sub
	}
	
	private static long consumeMaxInterval = -1;
	private static int consumeMaxRetryTimes = -1;

	private String groupName;
	private boolean loghandlerEnabled;
	private String namespacePrefix;
	
	private Boolean asyncConsumeEnabled;
	
	private static List<String>  ignoreLogTopics = new ArrayList<>();
	
	private static MQContext context = new MQContext();
	
	private MQLogHandler logHandler;
	
	//
	private volatile ThreadPoolExecutor logHandleExecutor;

	private MQContext() {}


	public static void close() {
		if(context.logHandleExecutor != null) {
			context.logHandleExecutor.shutdown();
			context.logHandleExecutor = null;
		}
	}
	
	
	private static MQLogHandler getLogHandler() {
		if(context.loghandlerEnabled && context.logHandler == null) {
			synchronized (context) {
				if(context.logHandler != null)return context.logHandler;
				MQLogHandler handler = InstanceFactory.getInstance(MQLogHandler.class);
				if(handler == null)throw new NullPointerException("not [MQLogHandler] define");
				context.logHandler = handler;
			}
		}
		return context.logHandler;
	}


	public static void setLogHandler(MQLogHandler logHandler) {
		context.logHandler = logHandler;
	}



	private static ThreadPoolExecutor getLogHandleExecutor() {
		if(context.logHandleExecutor != null)return context.logHandleExecutor;
		if(!context.loghandlerEnabled)return null;
		synchronized (context) {
			StandardThreadFactory threadFactory = new StandardThreadFactory("mqLogHandleExecutor");
			int nThread = ResourceUtils.getInt("mendmix.amqp.loghandler.threads", 2);
			int capacity = ResourceUtils.getInt("mendmix.amqp.loghandler.queueSize", 1000);
			context.logHandleExecutor = new ThreadPoolExecutor(nThread, nThread,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(capacity),
                    threadFactory);
			
			//忽略topic列表
			if(ResourceUtils.containsProperty("mendmix.amqp.loghandler.ignoreTopics")) {
				String[] topics = ResourceUtils.getProperty("mendmix.amqp.loghandler.ignoreTopics").split(",|;");
				for (String topic : topics) {
					ignoreLogTopics.add(rebuildWithNamespace(topic));
				}
			}
		}
		return context.logHandleExecutor;
	}


	public static String rebuildWithNamespace(String name){
    	if(context.namespacePrefix == null)return name;
    	if(name == null || name.startsWith(context.namespacePrefix))return name;
    	return context.namespacePrefix + name;
    }

	public static String getProviderName() {
		return ResourceUtils.getAndValidateProperty("mendmix.amqp.provider");
	}
	
	public static String getGroupName() {
		if(context.groupName == null) {
			String namespace = ResourceUtils.getProperty("mendmix.amqp.namespace");
			if(StringUtils.isNotBlank(namespace) && !"none".equals(namespace)){
				context.namespacePrefix = namespace + "_";
			}
			context.groupName = rebuildWithNamespace(ResourceUtils.getAndValidateProperty("mendmix.amqp.groupName"));
			context.loghandlerEnabled = Boolean.parseBoolean(ResourceUtils.getProperty("mendmix.amqp.loghandler.enabled", "true"));
		}
		return context.groupName;
	}
	
	public static boolean isProducerEnabled() {
		return Boolean.parseBoolean(ResourceUtils.getProperty("mendmix.amqp.producer.enabled", "true"));
	}
	
	public static boolean isConsumerEnabled() {
		return Boolean.parseBoolean(ResourceUtils.getProperty("mendmix.amqp.consumer.enabled", "false"));
	}
	
	/**
	 * 是否异步处理消息
	 * @return
	 */
	public static boolean isAsyncConsumeEnabled() {
		if(context.asyncConsumeEnabled != null)return context.asyncConsumeEnabled;
		return context.asyncConsumeEnabled = Boolean.parseBoolean(ResourceUtils.getProperty("mendmix.amqp.consumer.async.enabled", "true"));
	}
	
	public static boolean isLogEnabled() {
		return context.loghandlerEnabled;
	}

	public static int getMaxProcessThreads() {
		return ResourceUtils.getInt("mendmix.amqp.consumer.processThreads", 20);
	}
	
	public static long getConsumeMaxInterval() {
		if(consumeMaxInterval < 0) {
			consumeMaxInterval = ResourceUtils.getLong("mendmix.amqp.consume.maxInterval.ms",24 * 3600 * 1000);
		}
		return consumeMaxInterval;
	}


	public static int getConsumeMaxRetryTimes() {
		if(consumeMaxRetryTimes < 0) {
			consumeMaxRetryTimes = ResourceUtils.getInt("mendmix.amqp.consume.maxRetryTimes",10);
		}
		return consumeMaxRetryTimes;
	}


	public static void processMessageLog(MQMessage message,ActionType actionType,Throwable ex){
		if(!MQContext.isLogEnabled())return;
		ThreadPoolExecutor executor = MQContext.getLogHandleExecutor();
		if(ignoreLogTopics.contains(message.getTopic()))return;
		message.setProcessTime(System.currentTimeMillis());
		executor.execute(new Runnable() {
			@Override
			public void run() {
				if(ex == null) {
					getLogHandler().onSuccess(getGroupName(),actionType,message);
				}else {
					getLogHandler().onError(getGroupName(),actionType, message, ex);
				}
			}
		});
	}

}
