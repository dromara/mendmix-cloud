package com.mendmix.amqp.adapter.redis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import com.mendmix.amqp.MQConsumer;
import com.mendmix.amqp.MQContext;
import com.mendmix.amqp.MessageHandler;
import com.mendmix.common.async.StandardThreadExecutor;
import com.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import com.mendmix.spring.InstanceFactory;

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
		
	private MQContext context;
	private Map<String, MessageHandler> messageHandlers = new HashMap<>(); 
	/**
	 * @param messageHandlers
	 */
	public RedisConsumerAdapter(MQContext context,Map<String, MessageHandler> messageHandlers) {
		this.context = context;
		this.connectionFactory = InstanceFactory.getInstance(RedisConnectionFactory.class);
		this.messageHandlers = messageHandlers;
	}

	@Override
	public void start() throws Exception {
		int maxThread = context.getMaxProcessThreads();
		this.fetchExecutor = new ThreadPoolExecutor(1, 1,0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),new StandardThreadFactory("messageFetcher"));
		this.asyncProcessExecutor = new StandardThreadExecutor(1, maxThread,60, TimeUnit.SECONDS,1000,new StandardThreadFactory("messageAsyncProcessor"));
		container.setConnectionFactory(connectionFactory);
        container.setSubscriptionExecutor(fetchExecutor);
        container.setTaskExecutor(asyncProcessExecutor);
        //
        Set<String> topics = messageHandlers.keySet();
        MessageListenerAdapter listener;
        for (String topic : topics) {
        	MessageHandlerDelegate delegate = new MessageHandlerDelegate(context,topic, messageHandlers.get(topic));
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
