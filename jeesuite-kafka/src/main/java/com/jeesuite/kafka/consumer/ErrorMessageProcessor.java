/**
 * 
 */
package com.jeesuite.kafka.consumer;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.concurrent.StandardThreadExecutor.StandardThreadFactory;
import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.kafka.consumer.hanlder.RetryErrorMessageHandler;
import com.jeesuite.kafka.handler.MessageHandler;
import com.jeesuite.kafka.message.DefaultMessage;

/**
 * 消费者端处理错误消息重试处理器
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月25日
 */
public class ErrorMessageProcessor implements Closeable{
	
	private static final Logger logger = LoggerFactory.getLogger(ErrorMessageProcessor.class);
	
	//重试时间间隔单元（毫秒）
	private long retryPeriodUnit;
	private int maxReties;
	private RetryErrorMessageHandler retryErrorHandler;

	private final PriorityBlockingQueue<PriorityTask> taskQueue = new PriorityBlockingQueue<PriorityTask>(1000);  
	
	private ExecutorService executor;
	
	private AtomicBoolean closed = new AtomicBoolean(false);

	public int getRetryTaskNums(){
		return taskQueue.size();
	}

	public ErrorMessageProcessor(int poolSize,int retryPeriodSeconds,int maxReties,RetryErrorMessageHandler retryErrorHandler) {
		
		this.retryPeriodUnit = retryPeriodSeconds * 1000;
		this.maxReties = maxReties;
		this.retryErrorHandler = retryErrorHandler;
		executor = Executors.newFixedThreadPool(poolSize, new StandardThreadFactory("ErrorMessageProcessor"));
		executor.execute(new Runnable() {
			@Override
			public void run() {
				while(!closed.get()){
					try {
						PriorityTask task = taskQueue.take();
						//空任务跳出循环
						if(task.getMessage() == null)break;
						if(task.nextFireTime - System.currentTimeMillis() > 0){
							TimeUnit.MILLISECONDS.sleep(1000);
							taskQueue.put(task);
							continue;
						}
						task.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public void submit(final DefaultMessage message,final MessageHandler messageHandler){
		int taskCount;
		if((taskCount = taskQueue.size()) > 1000){
			logger.warn("ErrorMessageProcessor queue task count over:{}",taskCount);
		}
		taskQueue.add(new PriorityTask(message, messageHandler));
	}
	
	public void close(){
		closed.set(true);
		//taskQueue里面没有任务会一直阻塞，所以先add一个新任务保证执行
		taskQueue.add(new PriorityTask(null, null));
		try {Thread.sleep(1000);} catch (Exception e) {}
		executor.shutdown();
		logger.info("ErrorMessageDefaultProcessor closed");
	}
	
	class PriorityTask implements Runnable,Comparable<PriorityTask>{

		final DefaultMessage message;
		final MessageHandler messageHandler;
		
		int retryCount = 0;
	    long nextFireTime;
		
	    public PriorityTask(DefaultMessage message, MessageHandler messageHandler) {
	    	this(message, messageHandler, System.currentTimeMillis() + retryPeriodUnit);
	    }
	    
		public PriorityTask(DefaultMessage message, MessageHandler messageHandler,long nextFireTime) {
			super();
			this.message = message;
			this.messageHandler = messageHandler;
			this.nextFireTime = nextFireTime;
		}

		public DefaultMessage getMessage() {
			return message;
		}

		@Override
		public void run() {
			try {	
				logger.debug("begin re-process message:"+message.getMsgId());
				messageHandler.p2Process(message);
			} catch (Exception e) {
				retryCount++;
				logger.warn("retry[{}] mssageId[{}] error",retryCount,message.getMsgId());
				retry();
			}
		}
		
		private void retry(){
			if(retryCount == maxReties){
				if(retryErrorHandler != null){
					try {
						retryErrorHandler.process(ConsumerContext.getInstance().getGroupId(),message.topic(), message);
					} catch (Exception e) {
						logger.warn("persistHandler error,topic["+message.topic()+"]",e);
					}
				}else{					
					logger.warn("retry_skip process message[{}] maxReties over {} time error!!!",JsonUtils.toJson(message),maxReties);
				}
				return;
			}
			nextFireTime = nextFireTime + retryCount * retryPeriodUnit;
			//重新放入任务队列
			taskQueue.add(this);
			logger.debug("retry_resubmit mssageId[{}] task to queue,next fireTime:{}",this.message.getMsgId(),nextFireTime);
		}

		@Override
		public int compareTo(PriorityTask o) {
			return (int) (this.nextFireTime - o.nextFireTime);
		}

		@Override
		public String toString() {
			return "PriorityTask [message=" + message.getMsgId() + ", messageHandler=" + messageHandler.getClass().getSimpleName() + ", retryCount="
					+ retryCount + ", nextFireTime=" + nextFireTime + "]";
		}
		
	}

}

