/**
 * 
 */
package com.jeesuite.kafka.producer.handler;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.I0Itec.zkclient.ZkClient;
import org.apache.kafka.clients.producer.RecordMetadata;

import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.utils.NodeNameHolder;

import kafka.utils.ZKStringSerializer$;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月10日
 */
public class SendCounterHandler implements ProducerEventHandler {

	// <topic,[success,error]>
	private Map<String, AtomicLong[]> producerStats = new HashMap<>();

	private int currentStatHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); // 当前统计的小时

	private ZkClient zkClient;
	//
	private ScheduledExecutorService statScheduler;
	
	private String producerGroup;

	public SendCounterHandler(String producerGroup, String zkServers) {
		this.producerGroup = producerGroup == null ? NodeNameHolder.getNodeId() : producerGroup;
		int sessionTimeoutMs = 10000;
		int connectionTimeoutMs = 10000;
		zkClient = new ZkClient(zkServers, sessionTimeoutMs, connectionTimeoutMs, ZKStringSerializer$.MODULE$);
		//
		initCollectionTimer();
	}

	@Override
	public void onSuccessed(String topicName, RecordMetadata metadata) {
		updateProducerStat(topicName, false);
	}

	@Override
	public void onError(String topicName, DefaultMessage message, boolean isAsynSend) {
		updateProducerStat(topicName, true);
	}

	@Override
	public void close() throws IOException {
		statScheduler.shutdown();
		zkClient.close();
	}

	private void initCollectionTimer() {
		statScheduler = Executors.newScheduledThreadPool(1);
		statScheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				// 清除上一个小时的统计
				if (hourOfDay != currentStatHourOfDay) {
					producerStats.clear();
				}
			}
		}, 1, 5, TimeUnit.SECONDS);
	}

	private void updateProducerStat(String topic, boolean error) {
		if (!producerStats.containsKey(topic)) {
			synchronized (producerStats) {
				producerStats.put(topic, new AtomicLong[] { new AtomicLong(0), new AtomicLong(0) });
			}
		}
		if (!error) {
			producerStats.get(topic)[0].incrementAndGet();
		} else {
			producerStats.get(topic)[1].incrementAndGet();
		}
	}

	/**
	 *  kafka生产者统计
	 * 
	 * @return [topic -> [successCount,errorCount]]
	 */
	private Map<String, Long[]> producerStats() {
		Map<String, Long[]> result = new HashMap<>();
		Set<String> keys = producerStats.keySet();
		for (String key : keys) {
			AtomicLong[] v = producerStats.get(key);
			result.put(key, new Long[] { v[0].get(), v[1].get() });
		}
		return result;
	}

}
