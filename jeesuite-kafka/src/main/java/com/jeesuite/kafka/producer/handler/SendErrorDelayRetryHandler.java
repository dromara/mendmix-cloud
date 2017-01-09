/**
 * 
 */
package com.jeesuite.kafka.producer.handler;

import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.thread.StandardThreadExecutor.StandardThreadFactory;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月10日
 */
public class SendErrorDelayRetryHandler implements ProducerEventHandler{

	private static final Logger logger = LoggerFactory.getLogger(SendErrorDelayRetryHandler.class);

    private final PriorityBlockingQueue<PriorityTask> taskQueue = new PriorityBlockingQueue<PriorityTask>(1000);  
    
    private List<String> messageIdsInQueue = new Vector<>();
	
	private ExecutorService executor;
	
	private KafkaProducer<String, Object> topicProducer;
	
	private int retries = 0; //重试次数
	
	public SendErrorDelayRetryHandler(String producerGroup,KafkaProducer<String, Object> topicProducer,int retries) {
		this.topicProducer = topicProducer;
		this.retries = retries;
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
	
	@Override
	public void onSuccessed(String topicName, RecordMetadata metadata) {}

	@Override
	public void onError(String topicName, DefaultMessage message, boolean isAsynSend) {
		if(isAsynSend == false){
			return;
		}
		//在重试队列不处理
		if(messageIdsInQueue.contains(message.getMsgId()))return;
		//
		taskQueue.add(new PriorityTask(topicName,message));
		messageIdsInQueue.add(message.getMsgId());
	}
	
	@Override
	public void close() throws IOException {
		executor.shutdownNow();
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
				Object sendContent = message.isSendBodyOnly() ? message.getBody() : message;
				topicProducer.send(new ProducerRecord<String, Object>(topicName, message.getMsgId(),sendContent));
				//处理成功移除
				messageIdsInQueue.remove(message.getMsgId());
			} catch (Exception e) {
				logger.warn("retry mssageId[{}] error",message.getMsgId(),e);
				retry();
			}
		}
		
		private void retry(){
			if(retryCount == retries){
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
