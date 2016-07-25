/**
 * 
 */
package com.jeesuite.kafka.consumer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.DefaultManagedAwareThreadFactory;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.kafka.handler.MessageHandler;
import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.serializer.MessageDecoder;
import com.jeesuite.kafka.utils.KafkaConst;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年7月1日
 */
public class OldApiTopicConsumer implements TopicConsumer {

	private final static Logger logger = LoggerFactory.getLogger(OldApiTopicConsumer.class);
	
	private ConsumerConnector connector;
	//
	private Map<String, MessageHandler> topics;
	//接收线程
	private ThreadPoolExecutor fetchExecutor;
	//默认处理线程
	private ThreadPoolExecutor defaultProcessExecutor;
	
	private static final int DEFAULT_PROCESS_THREADS = 50;
	
	private AtomicBoolean runing = new AtomicBoolean(false);

	public OldApiTopicConsumer(ConsumerConnector connector, Map<String, MessageHandler> topics) {
		Validate.notNull(connector);

		this.connector = connector;
		this.topics = topics;
		
		int poolSize = topics.size();
		this.fetchExecutor = new ThreadPoolExecutor(poolSize, poolSize, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(), //
				new DefaultManagedAwareThreadFactory()) {
			protected void afterExecute(Runnable r, Throwable t) {
				super.afterExecute(r, t);
				printException(r, t);
			}
		};
		
		int processPoolSize = Integer.parseInt(ResourceUtils.get(KafkaConst.PROP_PROCESS_THREADS,String.valueOf(DEFAULT_PROCESS_THREADS)));
		this.defaultProcessExecutor = new ThreadPoolExecutor(processPoolSize, processPoolSize, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(1000), //
				new DefaultManagedAwareThreadFactory(),new ThreadPoolExecutor.CallerRunsPolicy()) {
			protected void afterExecute(Runnable r, Throwable t) {
				super.afterExecute(r, t);
				printException(r, t);
			}
		};
		
		logger.info("Kafka Conumer ThreadPool initialized,fetchPool Size:{},defalutProcessPool Size:{} ",poolSize,processPoolSize);
	}


	@Override
	public void start() {
		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		for (String topicName : topics.keySet()) {
			int nThreads = 1;
			topicCountMap.put(topicName, nThreads);
			logger.info("topic[{}] assign fetch Threads {}",topicName,nThreads);
		}
		
		StringDecoder keyDecoder = new StringDecoder(new VerifiableProperties());
		MessageDecoder valueDecoder = new MessageDecoder();

		Map<String, List<KafkaStream<String, Object>>> consumerMap = this.connector.createMessageStreams(topicCountMap,
				keyDecoder, valueDecoder);

		for (String topicName : topics.keySet()) {
			final List<KafkaStream<String, Object>> streams = consumerMap.get(topicName);

			for (final KafkaStream<String, Object> stream : streams) {
				MessageProcessor processer = new MessageProcessor(topicName, stream);
				this.fetchExecutor.execute(processer);
			}
		}
		//
		runing.set(true);
	}

	/**
	 * 线程池内异常处理
	 * @param r
	 * @param t
	 */
	private static void printException(Runnable r, Throwable t) {
		if (t == null && r instanceof Future<?>) {
			try {
				Future<?> future = (Future<?>) r;
				if (future.isDone())
					future.get();
			} catch (CancellationException ce) {
				t = ce;
			} catch (ExecutionException ee) {
				t = ee.getCause();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt(); // ignore/reset
			}
		}
		if (t != null)
			logger.error(t.getMessage(), t);
	}

	/**
	 * 消息处理器
	 */
	class MessageProcessor implements Runnable {

		KafkaStream<String, Object> stream;

		private String topicName;
		
		private MessageHandler messageHandler;
		
		private String processorName;
		public MessageProcessor(String topicName, KafkaStream<String, Object> stream) {
			this.stream = stream;
			this.topicName = topicName;
			this.messageHandler = topics.get(topicName);
			this.processorName = this.messageHandler.getClass().getName();
		}

		@Override
		public void run() {
 
			if (logger.isInfoEnabled()) {
				logger.info("MessageProcessor [{}] start, topic:{}",Thread.currentThread().getName(),topicName);
			}

			ConsumerIterator<String, Object> it = stream.iterator();
			// 没有消息的话，这里会阻塞
			while (it.hasNext()) {
				//该主题当前未消费的任务数
				long queueSize = defaultProcessExecutor.getQueue().size();
				//如果处理不过来，则抓取线程阻塞。
				// 阻塞时间规则：按条一秒递增,最多休眠30秒
				int sleepSec = ( sleepSec = (int) (queueSize/defaultProcessExecutor.getMaximumPoolSize()) - 1) > 30 ? 30 : sleepSec;
				if(sleepSec > 0){					
					try {Thread.sleep(TimeUnit.SECONDS.toMillis(sleepSec));} catch (Exception e) {}
				}
				try {					
					final DefaultMessage message = (DefaultMessage) it.next().message();
					//第一阶段处理
					messageHandler.p1Process(message);
					//第二阶段处理
					submitMessageToProcess(topicName,message);
				} catch (Exception e) {
					logger.error("received_topic_error,topic:"+topicName,e);
				}
				
			}
		
		}
		
		/**
		 * 提交消息到处理线程队列
		 * @param message
		 */
		private void submitMessageToProcess(final String topicName,final DefaultMessage message) {
			defaultProcessExecutor.submit(new Runnable() {
				@Override
				public void run() {
					try {	
						long start = logger.isDebugEnabled() ? System.currentTimeMillis() : 0;
						messageHandler.p2Process(message);
						if(logger.isDebugEnabled()){
							long useTime = System.currentTimeMillis() - start;
							if(useTime > 1000)logger.debug("received_topic_useTime [{}]process topic:{} use time {} ms",processorName,topicName,useTime);
						}
					} catch (Exception e) {
						logger.error("received_topic_process_error ["+processorName+"]processMessage error,topic:"+topicName,e);
					}
				}
			});
		}
		
	}

	@Override
	public void close() {
		if(!runing.get())return;
		this.fetchExecutor.shutdownNow();
		this.defaultProcessExecutor.shutdown();
		this.connector.commitOffsets();
		this.connector.shutdown();
		runing.set(false);
		logger.info("KafkaTopicSubscriber shutdown ok...");
	}
	
}
