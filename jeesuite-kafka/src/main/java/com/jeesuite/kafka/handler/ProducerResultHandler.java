/**
 * 
 */
package com.jeesuite.kafka.handler;

import java.io.Closeable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.kafka.consumer.ErrorMessageDefaultProcessor;
import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.monitor.KafkaMonitor;
import com.jeesuite.kafka.producer.DefaultTopicProducer;
import com.jeesuite.kafka.thread.StandardThreadExecutor.StandardThreadFactory;

/**
 * 消息发送结果处理器
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class ProducerResultHandler implements Closeable{

	private static final Logger logger = LoggerFactory.getLogger(ErrorMessageDefaultProcessor.class);

    private final PriorityBlockingQueue<PriorityTask> taskQueue = new PriorityBlockingQueue<PriorityTask>(1000);  
    
    private List<String> messageIdsInQueue = new Vector<>();
	
	private ExecutorService executor;
	
	private DefaultTopicProducer topicProducer;
	
	public ProducerResultHandler(DefaultTopicProducer topicProducer) {
		this.topicProducer = topicProducer;
		
		executor = Executors.newFixedThreadPool(1, new StandardThreadFactory("ErrorMessageProcessor"));
		executor.submit(new Runnable() {
			@Override
			public void run() {
				long currentTimeMillis = System.currentTimeMillis();
				while(true){
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

	public void setTopicProducer(DefaultTopicProducer topicProducer) {
		this.topicProducer = topicProducer;
	}
	
	public void close(){
		executor.shutdownNow();
	}

	public void onSuccessed(String topicName, DefaultMessage message) {
		//计数统计
		//KafkaMonitor.getContext().updateProducerStat(topicName, false);
	}

	public void onError(String topicName, DefaultMessage message,boolean isAsynSend) {
		//计数统计
		//KafkaMonitor.getContext().updateProducerStat(topicName, true);
		//同步发送不重试
		if(isAsynSend == false){
			return;
		}
		//在重试队列不处理
		if(messageIdsInQueue.contains(message.getMsgId()))return;
		//
		taskQueue.add(new PriorityTask(topicName,message));
		messageIdsInQueue.add(message.getMsgId());
	}
	
	class PriorityTask implements Runnable,Comparable<PriorityTask>{

		final String topicName;
		final DefaultMessage message;
		
		int retryCount = 0;
	    long nextFireTime;
		
	    public PriorityTask(String topicName,DefaultMessage message) {
	    	this(topicName,message, System.currentTimeMillis());
	    }
	    
		public PriorityTask(String topicName,DefaultMessage message,long nextFireTime) {
			super();
			this.topicName = topicName;
			this.message = message;
			this.nextFireTime = nextFireTime;
		}

		@Override
		public void run() {
			try {	
				logger.debug("begin re process message:"+this.toString());
				topicProducer.publish(topicName, message, true);
				//处理成功移除
				messageIdsInQueue.remove(message.getMsgId());
			} catch (Exception e) {
				logger.warn("retry mssageId[{}] error",message.getMsgId(),e);
				retry();
			}
		}
		
		private void retry(){
			if(retryCount == 3){
				return;
			}
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
			return "PriorityTask [message=" + message.getMsgId() + ", retryCount="
					+ retryCount + ", nextFireTime=" + nextFireTime + "]";
		}
		
	}
}
