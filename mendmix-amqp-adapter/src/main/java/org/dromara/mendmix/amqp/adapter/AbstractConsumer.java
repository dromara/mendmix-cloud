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
package org.dromara.mendmix.amqp.adapter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dromara.mendmix.amqp.MQConsumer;
import org.dromara.mendmix.amqp.MQContext;
import org.dromara.mendmix.amqp.MQContext.ActionType;
import org.dromara.mendmix.amqp.MQMessage;
import org.dromara.mendmix.amqp.MessageHandler;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.async.DelayRetryExecutor;
import org.dromara.mendmix.common.async.ICaller;
import org.dromara.mendmix.common.async.StandardThreadExecutor;
import org.dromara.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;

/**
 * 
 * <br>
 * Class Name   : AbstractConsumer
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Apr 25, 2021
 */
public abstract class AbstractConsumer implements MQConsumer {
	
	protected static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.amqp.adapter");
	
	protected Map<String, MessageHandler> messageHandlers;
	
	private AtomicBoolean closed = new AtomicBoolean(false);
	// 接收线程
	protected StandardThreadExecutor fetchExecutor;
	// 默认处理线程池
	protected StandardThreadExecutor asyncProcessExecutor;
	
	protected DelayRetryExecutor retryExecutor;
	//
	protected Semaphore semaphore;
	
	protected MQContext context;
	
	private long lastFetchErrLoggingTime;

	public AbstractConsumer(MQContext context,Map<String, MessageHandler> messageHandlers) {
		this.context = context;
		this.messageHandlers = messageHandlers;
	}

	protected void startWorker() {
		
		int fetchCoreThreads = 1; //异步处理拉取线程默认1
		int fetchMaxThreads = fetchCoreThreads;
		//异步处理
		if(context.isAsyncConsumeEnabled()) {
			int maxThread = context.getMaxProcessThreads();
			semaphore = new Semaphore(maxThread);
			this.asyncProcessExecutor = new StandardThreadExecutor(1, maxThread,60, TimeUnit.SECONDS,maxThread,new StandardThreadFactory("messageAsyncProcessor"));
		    //
			fetchMaxThreads = maxThread;
			logger.info("<startup-logging>  init asyncProcessExecutor finish -> maxThread:{}",maxThread);
		}
		//
		this.fetchExecutor = new StandardThreadExecutor(fetchCoreThreads, fetchMaxThreads,0, TimeUnit.SECONDS, fetchMaxThreads * 10,new StandardThreadFactory("messageFetcher"));
		fetchExecutor.execute(new Worker());
		
		//异步重试
		retryExecutor = new DelayRetryExecutor(1,5000, 1000, 3);
		
		logger.info("<startup-logging>  init fetchExecutor finish -> fetchMaxThreads:{}",fetchMaxThreads);
		
	}

	public abstract List<MQMessage> fetchMessages();
	
	public abstract String handleMessageConsumed(MQMessage message,boolean successed);
	
	protected void handleFetchError(Exception e) {
		long currentTime = System.currentTimeMillis();
		if(currentTime - lastFetchErrLoggingTime > 30000) {						
			logger.error("fetch_message_error",e);
			lastFetchErrLoggingTime = currentTime;
		}
	    try {Thread.sleep(100);} catch (Exception e2) {}    
	}
	
	
	/**
	 * @param message
	 * @param ex
	 */
	private void onMessageProcessComplated(MQMessage message,Exception ex){
		handleMessageConsumed(message,ex == null);
		MQContext.processMessageLog(context,message,ActionType.sub, ex);
	}
	
	/**
	 * 处理消息
	 * @param message
	 * @throws InterruptedException
	 */
	private void asyncConsumeMessage(MQMessage message) throws InterruptedException {
		
		
		if(context.getConsumeMaxRetryTimes() > 0 && message.getConsumeTimes() > context.getConsumeMaxRetryTimes()) {
			return;
		}
		
		//信号量获取通行证
		semaphore.acquire();
		asyncProcessExecutor.execute(new Runnable() {
			@Override
			public void run() {
				consumeMessage(message);
			}
			
		});
	}
	

	private void consumeMessage( MQMessage message) {
		MessageHandler messageHandler = messageHandlers.get(message.getTopic());
		try {	
			if(messageHandler == null) {
				logger.warn("not messageHandler found for:{}",message.getTopic());
				return;
			}
			//上游状态检查
			String stateCheckUrl = context.getStateCheckUrl();
			if(!message.originStatusCompleted(stateCheckUrl)) {
				retryExecutor.submit("message:"+message.getMsgId(), new ICaller<Void>() {
					@Override
					public Void call() throws Exception{
						if(message.originStatusCompleted(stateCheckUrl)) {
							messageHandler.process(message);
						}
						return null;
					}
				});
				logger.info("MQmessage_CONSUME_ABORT_ADD_RETRY -> message:{}",message.logString());
				return;
			}
			//用户上下文
			message.setUserContextOnConsume();
			//
			messageHandler.process(message);
			//处理成功，删除
			onMessageProcessComplated(message,null);
			if(logger.isDebugEnabled()) {
				logger.debug("MQmessage_CONSUME_SUCCESS -> message:{}",message.logString());
			}
		}catch (Exception e) {
			logger.error(String.format("MQmessage_CONSUME_ERROR -> [%s]",message.logString()),e);
			//异步重试
			if(messageHandler.retrieable()) {
				retryExecutor.submit("message:"+message.getMsgId(), new ICaller<Void>() {
					@Override
					public Void call() throws Exception{
						messageHandler.process(message);
						onMessageProcessComplated(message,null);
						return null;
					}
				});
			}else {
				onMessageProcessComplated(message,e);
			}
		} finally {
			ThreadLocalContext.unset();
			//释放信号量
			if(semaphore != null) {
				semaphore.release();
			}
		}
	}
	
	@Override
	public void shutdown() {
		closed.set(true);
		if(fetchExecutor != null) {
			fetchExecutor.shutdown();
		}
		if(asyncProcessExecutor != null) {
			asyncProcessExecutor.shutdown();
		}
		
		if(retryExecutor != null) {
			retryExecutor.close();
		}
	}

	private class Worker implements Runnable{

		@Override
		public void run() {
			while(!closed.get()){ 
				try {
					if(GlobalContext.isStarting()) {
						Thread.sleep(10);
						continue;
					}
					if(asyncProcessExecutor != null && asyncProcessExecutor.getSubmittedTasksCount() >= context.getMaxProcessThreads()) {
						Thread.sleep(1);
						continue;
					}
					List<MQMessage> messages = fetchMessages();
					if(messages == null || messages.isEmpty()){
						Thread.sleep(10);
						continue;
					}
					mloop:for (MQMessage message : messages) {
						if(!context.matchedOnFilter(message))continue mloop;
						if(asyncProcessExecutor == null) {
							consumeMessage(message);
						}else {
							asyncConsumeMessage(message);
						}
					}
				} catch (Exception e) {
					handleFetchError(e);
				}
			}
		}
		
	}
}
