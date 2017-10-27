/**
 * 
 */
package com.jeesuite.kafka.producer.handler;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.I0Itec.zkclient.ZkClient;
import org.apache.kafka.clients.producer.RecordMetadata;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.NodeNameHolder;
import com.jeesuite.kafka.message.DefaultMessage;
import com.jeesuite.kafka.monitor.model.ProducerStat;
import com.jeesuite.kafka.serializer.ZKStringSerializer;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月10日
 */
public class SendCounterHandler implements ProducerEventHandler {

	public static String ROOT = "/producers";
	// <topic,[total_success,total_error,latest_success,latest_error]>
	private Map<String, AtomicLong[]> producerStats = new HashMap<>();

	private int currentStatHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); // 当前统计的小时

	private ZkClient zkClient;
	//
	private ScheduledExecutorService statScheduler;
	
	private String groupPath;
	private Map<String, String> statPaths = new HashMap<>();
	
	private String producerGroup;
	
	private AtomicBoolean commited = new AtomicBoolean(false);

	public SendCounterHandler(String producerGroup, String zkServers) {
		this.producerGroup =producerGroup;
		int sessionTimeoutMs = 10000;
		int connectionTimeoutMs = 10000;
		zkClient = new ZkClient(zkServers, sessionTimeoutMs, connectionTimeoutMs, new ZKStringSerializer());
		//
		groupPath = ROOT + "/" + producerGroup;
		if(!zkClient.exists(groupPath)){
			zkClient.createPersistent(groupPath, true);
		}
		initCollectionTimer();
	}

	@Override
	public void onSuccessed(String topicName, RecordMetadata metadata) {
		try {			
			updateProducerStat(topicName, false);
		} catch (Exception e) {}
	}

	@Override
	public void onError(String topicName, DefaultMessage message, boolean isAsynSend) {
        try {			
        	updateProducerStat(topicName, true);
		} catch (Exception e) {}
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
				if (hourOfDay != currentStatHourOfDay) {
					// 清除上一个小时的统计
					Collection<AtomicLong[]> values = producerStats.values();
					for (AtomicLong[] nums : values) {
						nums[2].set(0);
						nums[3].set(0);
					}
				}
				//
				commitToZK();
			}
		}, 5, 5, TimeUnit.SECONDS);
	}
	
	private void commitToZK(){
		if(commited.get())return;
		Set<Entry<String, AtomicLong[]>> entrySet = producerStats.entrySet();
		for (Entry<String, AtomicLong[]> entry : entrySet) {
			AtomicLong[] nums = entry.getValue();
			ProducerStat stat = new ProducerStat(entry.getKey(), producerGroup, nums[0], nums[1], nums[2], nums[3]);
			zkClient.writeData(statPaths.get(entry.getKey()), JsonUtils.toJson(stat));
		}
		commited.set(true);
	}

	private void updateProducerStat(String topic, boolean error) {
		if (!producerStats.containsKey(topic)) {
			synchronized (producerStats) {
				String path = groupPath + "/" + topic;
				if(!zkClient.exists(path)){
					zkClient.createPersistent(path, true);
				}
				//节点临时目录
				path = path + "/" + NodeNameHolder.getNodeId();
				zkClient.createEphemeral(path);
				statPaths.put(topic, path);
				producerStats.put(topic, new AtomicLong[] { new AtomicLong(0), new AtomicLong(0), new AtomicLong(0), new AtomicLong(0) });
			}
		}
		if (!error) {
			producerStats.get(topic)[0].incrementAndGet();
			producerStats.get(topic)[2].incrementAndGet();
		} else {
			producerStats.get(topic)[1].incrementAndGet();
			producerStats.get(topic)[3].incrementAndGet();
		}
		
		commited.set(false);
	}
	
	public static void main(String[] args) {
		Map<String, AtomicLong[]> stats = new HashMap<>();
		stats.put("test", new AtomicLong[] { new AtomicLong(4), new AtomicLong(5), new AtomicLong(5), new AtomicLong(5) });
	
		Collection<AtomicLong[]> values = stats.values();
		for (AtomicLong[] nums : values) {
			nums[2].set(0);
			nums[3].set(0);
		}
		
		AtomicLong[] longs = stats.get("test");
		System.out.println(longs);
	}

}
