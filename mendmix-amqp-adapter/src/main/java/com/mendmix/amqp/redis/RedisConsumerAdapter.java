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
package com.mendmix.amqp.redis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import com.mendmix.amqp.MQConsumer;
import com.mendmix.amqp.MQContext;
import com.mendmix.amqp.MessageHandler;
import com.mendmix.cache.RedisTemplateGroups;
import com.mendmix.common.async.StandardThreadExecutor;
import com.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年5月8日
 */
public class RedisConsumerAdapter implements MQConsumer {

	private RedisConnectionFactory connectionFactory;
	private RedisMessageListenerContainer container = new RedisMessageListenerContainer();
	private ThreadPoolExecutor fetchExecutor;
	private StandardThreadExecutor asyncProcessExecutor;
		
	private Map<String, MessageHandler> messageHandlers = new HashMap<>(); 
	/**
	 * @param messageHandlers
	 */
	public RedisConsumerAdapter(Map<String, MessageHandler> messageHandlers) {
		this.messageHandlers = messageHandlers;
	}

	@Override
	public void start() throws Exception {
		StringRedisTemplate redisTemplate = RedisTemplateGroups.getDefaultStringRedisTemplate();
		Validate.notNull(redisTemplate, "can't load bean [redisTemplate]");
		this.connectionFactory = redisTemplate.getConnectionFactory();
		int maxThread = MQContext.getMaxProcessThreads();
		this.fetchExecutor = new ThreadPoolExecutor(1, 1,0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),new StandardThreadFactory("messageFetcher"));
		this.asyncProcessExecutor = new StandardThreadExecutor(1, maxThread,60, TimeUnit.SECONDS,1000,new StandardThreadFactory("messageAsyncProcessor"));
		container.setConnectionFactory(connectionFactory);
        container.setSubscriptionExecutor(fetchExecutor);
        container.setTaskExecutor(asyncProcessExecutor);
        //
        Set<String> topics = messageHandlers.keySet();
        MessageListenerAdapter listener;
        for (String topic : topics) {
        	MessageHandlerDelegate delegate = new MessageHandlerDelegate(topic, messageHandlers.get(topic));
        	listener = new MessageListenerAdapter(delegate, "onMessage");
        	listener.afterPropertiesSet();
        	container.addMessageListener(listener, new PatternTopic(topic));
		}
        
        container.afterPropertiesSet();
        container.start();
	}

	@Override
	public void shutdown() {
		fetchExecutor.shutdown();
		asyncProcessExecutor.shutdown();
		container.stop();
		try {container.destroy();} catch (Exception e) {}
	}

	

}
