package com.jeesuite.kafka.consumer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.kafka.handler.MessageHandler;
import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.thread.StandardThreadExecutor;

/**
 * 默认消费者实现（new consumer api）
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月12日
 */
public class NewApiTopicConsumer implements TopicConsumer {

	private static final Logger logger = LoggerFactory.getLogger(ConsumerWorker.class);

	private Properties configs;
	private Map<String, MessageHandler> topicHandlers;
	
	private ExecutorService fetcheExecutor;
	private StandardThreadExecutor processExecutor;
	private List<ConsumerWorker<String, DefaultMessage>> consumers = new ArrayList<>();

	
	public NewApiTopicConsumer(Properties configs, Map<String, MessageHandler> topicHandlers,int maxProcessThreads) {
		super();
		this.configs = configs;
		this.topicHandlers = topicHandlers;
		//
	    fetcheExecutor = Executors.newFixedThreadPool(topicHandlers.size());
	    //
	    processExecutor = new StandardThreadExecutor(1, maxProcessThreads, 1000);
	}

	@Override
	public void start() {

		for (int i = 0; i < topicHandlers.size(); i++) {
			ConsumerWorker<String, DefaultMessage> consumer = new ConsumerWorker<>(configs, topicHandlers,processExecutor);
			consumers.add(consumer);
			fetcheExecutor.submit(consumer);
		}
	}

	@Override
	public void close() {
		for (int i = 0; i < consumers.size(); i++) {
			consumers.get(i).close();
			consumers.remove(i);
			i--;
		}
		fetcheExecutor.shutdown();
		processExecutor.shutdown();
	}
	
	private class ConsumerWorker<K extends Serializable, V extends DefaultMessage> implements Runnable {

		private KafkaConsumer<K, V> consumer;
		private final String clientId;
		private Map<String, MessageHandler> topicProcessers;
		
		private ExecutorService processExecutor;

		private AtomicBoolean closed = new AtomicBoolean();
		private CountDownLatch shutdownLatch = new CountDownLatch(1);

		public ConsumerWorker(Properties configs, Map<String, MessageHandler> topicProcessers,ExecutorService processExecutor) {

			this.clientId = configs.getProperty(ConsumerConfig.CLIENT_ID_CONFIG);
			this.topicProcessers = topicProcessers;
			configs.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
			this.consumer = new KafkaConsumer<>(configs);
			this.processExecutor = processExecutor;
		}

		@Override
		public void run() {

			logger.info("Starting consumer : {}", clientId);

			ExecutorService executor = Executors.newFixedThreadPool(1);
			final Map<TopicPartition, Long> partitionToUncommittedOffsetMap = new ConcurrentHashMap<>();
			final List<Future<Boolean>> futures = new ArrayList<>();

			ConsumerRebalanceListener listener = new ConsumerRebalanceListener() {

				@Override
				public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
					if (!futures.isEmpty())
						futures.get(0).cancel(true);

					logger.info("C : {}, Revoked topicPartitions : {}", clientId, partitions);
					commitOffsets(partitionToUncommittedOffsetMap);
				}

				@Override
				public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
					for (TopicPartition tp : partitions) {
						OffsetAndMetadata offsetAndMetaData = consumer.committed(tp);
						long startOffset = offsetAndMetaData != null ? offsetAndMetaData.offset() : -1L;
						logger.info("C : {}, Assigned topicPartion : {} offset : {}", clientId, tp, startOffset);

						if (startOffset >= 0)
							consumer.seek(tp, startOffset);
					}
				}
			};

			List<String> topics = new ArrayList<>(topicProcessers.keySet());
			consumer.subscribe(topics, listener);
			logger.info("Started to process records for consumer : {}", clientId);

			while (!closed.get()) {

				ConsumerRecords<K, V> records = consumer.poll(1500);

				// no record found
				if (records.isEmpty()) {
					continue;
				}

				//由于处理消息可能产生延时，收到消息后，暂停所有分区并手动发送心跳，以避免consumer group被踢掉
				consumer.pause(consumer.assignment());
				Future<Boolean> future = executor.submit(new ConsumeRecords(records, partitionToUncommittedOffsetMap));
				futures.add(future);

				Boolean isCompleted = false;
				while (!isCompleted && !closed.get()) {
					try {
						//等待 heart-beat 间隔时间
						isCompleted = future.get(3, TimeUnit.SECONDS); 
					} catch (TimeoutException e) {
						logger.debug("C : {}, heartbeats the coordinator", clientId);
						consumer.poll(0); // does heart-beat
						commitOffsets(partitionToUncommittedOffsetMap);
					} catch (CancellationException e) {
						logger.debug("C : {}, ConsumeRecords Job got cancelled", clientId);
						break;
					} catch (ExecutionException | InterruptedException e) {
						logger.error("C : {}, Error while consuming records", clientId, e);
						break;
					}
				}
				futures.remove(future);
				consumer.resume(consumer.assignment());
				commitOffsets(partitionToUncommittedOffsetMap);
			}

			try {
				executor.shutdownNow();
				while (!executor.awaitTermination(5, TimeUnit.SECONDS))
					;
			} catch (InterruptedException e) {
				logger.error("C : {}, Error while exiting the consumer", clientId, e);
			}
			consumer.close();
			shutdownLatch.countDown();
			logger.info("C : {}, consumer exited", clientId);
		}

		private void commitOffsets(Map<TopicPartition, Long> partitionToOffsetMap) {

			if (!partitionToOffsetMap.isEmpty()) {
				Map<TopicPartition, OffsetAndMetadata> partitionToMetadataMap = new HashMap<>();
				for (Entry<TopicPartition, Long> e : partitionToOffsetMap.entrySet()) {
					partitionToMetadataMap.put(e.getKey(), new OffsetAndMetadata(e.getValue() + 1));
				}

				logger.info("C : {}, committing the offsets : {}", clientId, partitionToMetadataMap);
				consumer.commitSync(partitionToMetadataMap);
				partitionToOffsetMap.clear();
			}
		}

		public void close() {
			try {
				closed.set(true);
				shutdownLatch.await();
			} catch (InterruptedException e) {
				logger.error("Error", e);
			}
		}

		private class ConsumeRecords implements Callable<Boolean> {

			ConsumerRecords<K, V> records;
			Map<TopicPartition, Long> partitionToUncommittedOffsetMap;

			public ConsumeRecords(ConsumerRecords<K, V> records,
					Map<TopicPartition, Long> partitionToUncommittedOffsetMap) {
				this.records = records;
				this.partitionToUncommittedOffsetMap = partitionToUncommittedOffsetMap;
			}

			@Override
			public Boolean call() {

				logger.info("C : {}, Number of records received : {}", clientId, records.count());
				try {
					for (final ConsumerRecord<K, V> record : records) {
						TopicPartition tp = new TopicPartition(record.topic(), record.partition());
						logger.info("C : {}, Record received topicPartition : {}, offset : {}", clientId, tp,record.offset());
						partitionToUncommittedOffsetMap.put(tp, record.offset());
						
						//第一阶段处理
						final MessageHandler messageHandler = topicProcessers.get(record.topic());
						messageHandler.p1Process(record.value());
						//第二阶段处理
						processExecutor.submit(new Runnable() {
							@Override
							public void run() {
								try {									
									messageHandler.p2Process(record.value());
								} catch (Exception e) {
									logger.error("["+messageHandler.getClass().getSimpleName()+"] process Topic["+record.topic()+"] error",e);
								}
							}
						});
					}
				} catch (Exception e) {
					logger.error("Error while consuming", e);
				}
				return true;
			}
		}

	}

}
