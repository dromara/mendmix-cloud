package com.jeesuite.kafka.consumer;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.kafka.thread.StandardThreadExecutor;
import com.jeesuite.kafka.thread.StandardThreadExecutor.StandardThreadFactory;

public abstract class AbstractTopicConsumer implements Closeable{
	
	private final static Logger logger = LoggerFactory.getLogger("com.jeesuite.kafka.consumer");

	protected ConsumerContext consumerContext;
	//接收线程
	protected StandardThreadExecutor fetchExecutor;
	//默认处理线程池
	protected StandardThreadExecutor defaultProcessExecutor;
	//高优先级处理线程池
	protected StandardThreadExecutor highProcessExecutor;
	//执行线程池满了被拒绝任务处理线程池
	protected ExecutorService poolRejectedExecutor = Executors.newSingleThreadExecutor();
	
	protected AtomicBoolean runing = new AtomicBoolean(false);
	
	public AbstractTopicConsumer(ConsumerContext context) {
		this.consumerContext = context;
		int poolSize = consumerContext.getMessageHandlers().size();
		for (String topicName : consumerContext.getMessageHandlers().keySet()) {
			context.updateConsumerStats(topicName, 0);
		}
		this.fetchExecutor = new StandardThreadExecutor(poolSize, poolSize,0, TimeUnit.SECONDS, poolSize,new StandardThreadFactory("KafkaFetcher"));
		
		this.defaultProcessExecutor = new StandardThreadExecutor(1, context.getMaxProcessThreads(),30, TimeUnit.SECONDS, context.getMaxProcessThreads(),new StandardThreadFactory("defaultProcessExecutor"),new PoolFullRunsPolicy());
		this.highProcessExecutor = new StandardThreadExecutor(1, 10,30, TimeUnit.SECONDS, context.getMaxProcessThreads(),new StandardThreadFactory("highProcessExecutor"),new PoolFullRunsPolicy());
		
		logger.info("Kafka Conumer ThreadPool initialized,fetchPool Size:{},defalutProcessPool Size:{} ",poolSize,context.getMaxProcessThreads());
	}
	
	
	@Override
	public void close() {
		this.fetchExecutor.shutdown();
		this.defaultProcessExecutor.shutdown();
		this.highProcessExecutor.shutdown();
		this.poolRejectedExecutor.shutdown();
		this.consumerContext.close();
		runing.set(false);
	}
	
	
	/**
	 * 处理线程满后策略
	 * @description <br>
	 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
	 * @date 2016年7月25日
	 */
	private class PoolFullRunsPolicy implements RejectedExecutionHandler {
		
        public PoolFullRunsPolicy() {}
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        	poolRejectedExecutor.execute(r);
        }
    }
}
