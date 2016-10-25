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

import com.jeesuite.kafka.handler.MessageHandler;
import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.thread.StandardThreadExecutor.StandardThreadFactory;

/**
 * 消费者端处理错误消息重试处理器
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月25日
 */
public class ErrorMessageDefaultProcessor implements Closeable{
	
	private static final Logger logger = LoggerFactory.getLogger(ErrorMessageDefaultProcessor.class);

	private final PriorityBlockingQueue<PriorityTask> taskQueue = new PriorityBlockingQueue<PriorityTask>(1000);  
	
	private ExecutorService executor;
	
	private AtomicBoolean closed = new AtomicBoolean(false);
	
	public ErrorMessageDefaultProcessor() {
		this(1);
	}

	public ErrorMessageDefaultProcessor(int poolSize) {
		executor = Executors.newFixedThreadPool(poolSize, new StandardThreadFactory("ErrorMessageProcessor"));
		executor.submit(new Runnable() {
			@Override
			public void run() {
				long currentTimeMillis = System.currentTimeMillis();
				while(!closed.get()){
					try {
						PriorityTask task = taskQueue.take();
						if(task.nextFireTime < currentTimeMillis){
							TimeUnit.MILLISECONDS.sleep(100);
							continue;
						}
						task.run();
					} catch (Exception e) {}
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
		//taskQueue 会一直阻塞，所以立即停止
		executor.shutdownNow();
		logger.info("ErrorMessageDefaultProcessor closed");
	}
	
	class PriorityTask implements Runnable,Comparable<PriorityTask>{

		final DefaultMessage message;
		final MessageHandler messageHandler;
		
		int retryCount = 0;
	    long nextFireTime;
		
	    public PriorityTask(DefaultMessage message, MessageHandler messageHandler) {
	    	this(message, messageHandler, System.currentTimeMillis());
	    }
	    
		public PriorityTask(DefaultMessage message, MessageHandler messageHandler,long nextFireTime) {
			super();
			this.message = message;
			this.messageHandler = messageHandler;
			this.nextFireTime = nextFireTime;
		}

		@Override
		public void run() {
			try {	
				logger.debug("begin re process message:"+this.toString());
				messageHandler.p2Process(message);
			} catch (Exception e) {
				logger.warn("retry mssageId[{}] error",message.getMsgId(),e);
				retry();
			}
		}
		
		private void retry(){
			if(retryCount == 3)return;
			nextFireTime = nextFireTime + retryCount * 30 * 1000;
			//重新放入任务队列
			taskQueue.add(this);
			logger.debug("re submit mssageId[{}] task to queue,next fireTime:",this.message.getMsgId(),nextFireTime);
			retryCount++;
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

