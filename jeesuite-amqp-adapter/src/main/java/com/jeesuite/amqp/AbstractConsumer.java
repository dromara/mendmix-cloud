package com.jeesuite.amqp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.amqp.MQContext.ActionType;
import com.jeesuite.common.ThreadLocalContext;
import com.jeesuite.common.async.StandardThreadExecutor;
import com.jeesuite.common.async.StandardThreadExecutor.StandardThreadFactory;
import com.jeesuite.common.util.ResourceUtils;

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
	
	protected static Logger logger = LoggerFactory.getLogger("com.jeesuite.amqp");
	
	protected Map<String, MessageHandler> messageHandlers;
	
	protected int batchSize;

	private AtomicBoolean closed = new AtomicBoolean(false);
	// 接收线程
	protected StandardThreadExecutor fetchExecutor;
	// 默认处理线程池
	protected StandardThreadExecutor asyncProcessExecutor;
	//
	protected Semaphore semaphore;

	public AbstractConsumer(Map<String, MessageHandler> messageHandlers) {
		this.messageHandlers = messageHandlers;
		this.batchSize = ResourceUtils.getInt("jeesuite.amqp.consumer.fetch.batchSize", 1);
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
			logger.info("init asyncProcessExecutor finish -> maxThread:{}",maxThread);
		}
		//
		this.fetchExecutor = new StandardThreadExecutor(fetchCoreThreads, fetchMaxThreads,0, TimeUnit.SECONDS, fetchMaxThreads * 10,new StandardThreadFactory("messageFetcher"));
		fetchExecutor.execute(new Worker());
		
		logger.info("init fetchExecutor finish -> fetchMaxThreads:{}",fetchMaxThreads);
		
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
			//多租户支持
			if(message.getTenantId() != null) {							
				ThreadLocalContext.set(ThreadLocalContext.TENANT_ID_KEY, message.getTenantId());
			}
			//事务消息检查
            if(message.getTransactionId() != null){
            	String transactionStatus = message.checkTransactionStatus();
            	if(transactionStatus != null) {
            		if(transactionStatus.equals(MessageStatus.processed.name())) {
						logger.info("MQmessage_TRANSACTION_STATUS_PROCESSED ->topic:{},requestId:{},transactionId:{}",message.getTopic(),message.getRequestId(),message.getTransactionId());
						//
						processMessageConsumeLog(message,null);
						return;
					}else if(transactionStatus.equals(MessageStatus.notExists.name())) {
						//考虑发起方事务提交可能延时等情况，这里开启一次重试
						if(message.getConsumeTimes() > 1) {
							logger.info("MQmessage_TRANSACTION_STATUS_INVALID ->topic:{},requestId:{},transactionId:{}",message.getTopic(),message.getRequestId(),message.getTransactionId());
							//
							processMessageConsumeLog(message,new IllegalArgumentException("transactionId["+message.getTransactionId()+"] not found"));
						}
						return;
					}
            	}
				if(logger.isDebugEnabled())logger.debug("MQmessage_TRANSACTION_STATUS_VALID -> topic:{},transactionId:{}",message.getTopic(),message.getTransactionId());
			}
			messageHandler.process(message);
			//处理成功，删除
			processMessageConsumeLog(message,null);
			if(logger.isDebugEnabled()) {
				logger.debug("MQmessage_CONSUME_SUCCESS -> message:{}",message.logString());
			}
		}catch (Exception e) {
			logger.error(String.format("MQmessage_CONSUME_ERROR -> [%s]",message.logString()),e);
			processMessageConsumeLog(message,e);
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
