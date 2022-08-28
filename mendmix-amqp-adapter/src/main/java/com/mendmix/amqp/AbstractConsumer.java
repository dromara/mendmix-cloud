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

import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.amqp.MQContext.ActionType;
import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.async.DelayRetryExecutor;
import com.mendmix.common.async.ICaller;
import com.mendmix.common.async.StandardThreadExecutor;
import com.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import com.mendmix.common.util.ResourceUtils;

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
	
	protected static Logger logger = LoggerFactory.getLogger("com.mendmix.amqp");
	
	protected Map<String, MessageHandler> messageHandlers;
	
	protected int batchSize;

	private AtomicBoolean closed = new AtomicBoolean(false);
	// 接收线程
	protected StandardThreadExecutor fetchExecutor;
	// 默认处理线程池
	protected StandardThreadExecutor asyncProcessExecutor;
	
	protected DelayRetryExecutor retryExecutor;
	//
	protected Semaphore semaphore;

	public AbstractConsumer(Map<String, MessageHandler> messageHandlers) {
		this.messageHandlers = messageHandlers;
		this.batchSize = ResourceUtils.getInt("mendmix.amqp.consumer.fetch.batchSize", 1);
	}

	protected void startWorker() {
		
		int fetchCoreThreads = 1; //异步处理拉取线程默认1
		int fetchMaxThreads = fetchCoreThreads;
		//异步处理
		if(MQContext.isAsyncConsumeEnabled()) {
			int maxThread = MQContext.getMaxProcessThreads();
			semaphore = new Semaphore(maxThread);
			this.asyncProcessExecutor = new StandardThreadExecutor(1, maxThread,60, TimeUnit.SECONDS,maxThread,new StandardThreadFactory("messageAsyncProcessor"));
		    //
			fetchMaxThreads = maxThread;
			logger.info("MENDMIX-TRACE-LOGGGING-->> init asyncProcessExecutor finish -> maxThread:{}",maxThread);
		}
		//
		this.fetchExecutor = new StandardThreadExecutor(fetchCoreThreads, fetchMaxThreads,0, TimeUnit.SECONDS, fetchMaxThreads * 10,new StandardThreadFactory("messageFetcher"));
		fetchExecutor.execute(new Worker());
		
		//异步重试
		retryExecutor = new DelayRetryExecutor(1,5000, 1000, 3);
		
		logger.info("MENDMIX-TRACE-LOGGGING-->> init fetchExecutor finish -> fetchMaxThreads:{}",fetchMaxThreads);
		
	}

	public abstract List<MQMessage> fetchMessages();
	
	public abstract String handleMessageConsumed(MQMessage message);
	
	
	/**
	 * 日志记录
	 * @param message
	 * @param ex
	 */
	private void processMessageConsumeLog(MQMessage message,Exception ex){
		if(ex == null) {			
			handleMessageConsumed(message);
		}
		MQContext.processMessageLog(message,ActionType.sub, ex);
	}
	
	/**
	 * 处理消息
	 * @param message
	 * @throws InterruptedException
	 */
	private void asyncConsumeMessage(MQMessage message) throws InterruptedException {
		
		if(MQContext.getConsumeMaxRetryTimes() > 0 && message.getConsumeTimes() > MQContext.getConsumeMaxRetryTimes()) {
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
	
	private void consumeMessage(MQMessage message) {
		MessageHandler messageHandler = messageHandlers.get(message.getTopic());
		try {	
			//上下文
			if(message.getHeaders() != null) {	
				CurrentRuntimeContext.addContextHeaders(message.getHeaders());
			}
			//消息状态检查
			if(!message.originStatusCompleted()) {
				if(message.getConsumeTimes() <= 1) {
					retryExecutor.submit("message:"+message.getMsgId(), new ICaller<Void>() {
						@Override
						public Void call() throws Exception{
							if(message.originStatusCompleted()) {
								messageHandler.process(message);
							}
							return null;
						}
					});
				}else {
					logger.info("MENDMIX-TRACE-LOGGGING-->> MQmessage_TRANSACTION_STATUS_INVALID ->topic:{},txId:{}",message.getTopic(),message.getTxId());
					processMessageConsumeLog(message,new IllegalArgumentException("txId["+message.getTxId()+"] not found"));
				}
				logger.info("MQmessage_CONSUME_ABORT_ADD_RETRY -> message:{}",message.logString());
				return;
        	}
			messageHandler.process(message);
			//处理成功，删除
			processMessageConsumeLog(message,null);
			if(logger.isDebugEnabled()) {
				logger.debug("MENDMIX-TRACE-LOGGGING-->> MQmessage_CONSUME_SUCCESS -> message:{}",message.logString());
			}
		}catch (Exception e) {
			logger.error(String.format("MENDMIX-TRACE-LOGGGING-->> MQmessage_CONSUME_ERROR -> [%s]",message.logString()),e);
			if(messageHandler.retrieable()) {
				retryExecutor.submit("message:"+message.getMsgId(), new ICaller<Void>() {
					@Override
					public Void call() throws Exception{
						messageHandler.process(message);
						return null;
					}
				});
			}else {
				processMessageConsumeLog(message,e);
			}
		} finally {
			ThreadLocalContext.unset();
			//释放信号量
			if(semaphore != null)semaphore.release();
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
	}

	private class Worker implements Runnable{

		@Override
		public void run() {
			while(!closed.get()){ 
				try {	
					if(asyncProcessExecutor != null && asyncProcessExecutor.getSubmittedTasksCount() >= MQContext.getMaxProcessThreads()) {
						Thread.sleep(1);
						continue;
					}
					List<MQMessage> messages = fetchMessages();
					if(messages == null || messages.isEmpty()){
						Thread.sleep(100);
						continue;
					}
					for (MQMessage message : messages) {
						if(asyncProcessExecutor == null) {
							consumeMessage(message);
						}else {
							asyncConsumeMessage(message);
						}
					}
				} catch (Exception e) {
					
				}
			}
		}
		
	}
}
