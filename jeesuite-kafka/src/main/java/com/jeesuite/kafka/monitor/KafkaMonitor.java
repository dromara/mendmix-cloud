/**
 * 
 */
package com.jeesuite.kafka.monitor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.kafka.utils.KafkaConst;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class KafkaMonitor {

	private KafkaZookeeperHelper zkHelper = new KafkaZookeeperHelper();
	
	private KafkaMonitorHelper kafkaHelper = new KafkaMonitorHelper();

	private static KafkaMonitor context = new KafkaMonitor();;
	
	private String consumerId;
	private List<TopicStat> topicStats = new ArrayList<>();
	
	//消费延时阀值
	private int latThreshold = 2000;
	
	private int currentStatHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); //当前统计的小时
	//<topic,[success,error]>
	private Map<String, AtomicLong[]> producerStats = new HashMap<>();
	
	//
	private Timer timer;
	
	//避免频繁查询，消费者统计统一管理
	private ConsumerGroupStat statCache;
	
	Lock lock = new ReentrantLock();// 锁 
	
	public static KafkaMonitor getContext() {
		return context;
	}

	private KafkaMonitor() {
		latThreshold = Integer.parseInt(ResourceUtils.get(KafkaConst.PROP_TOPIC_LAT_THRESHOLD, "2"));
		// 
		timer = new Timer(true);
		timer.schedule(new TimerTask() {
			
			public void run() {
				lock.lock();
				int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				try {
					//清除上一个小时的统计
					if(hourOfDay != currentStatHourOfDay){
						producerStats.clear();
					}
					//清除缓存
					statCache = null;
					//
				} finally {
					lock.unlock();
				}
			}
		}, 1000, 30*1000);
	}
	
	public void setConsumerInfo(String consumerId,List<String> topics) {
		this.consumerId = consumerId;
		for (String topic : topics) {
			TopicStat topicStat = new TopicStat();
			topicStat.setTopicName(topic);
			topicStats.add(topicStat);
		}
	}
	
	public void updateProducerStat(String topic,boolean error){
		if(!producerStats.containsKey(topic)){
			synchronized (producerStats) {
				producerStats.put(topic, new AtomicLong[]{new AtomicLong(0),new AtomicLong(0)});
			}
		}
		if(!error){
			producerStats.get(topic)[0].incrementAndGet();
		}else{
			producerStats.get(topic)[1].incrementAndGet();
		}
	}

	public ConsumerGroupStat consumerStats(String groupName){
		ConsumerGroupStat stat = statCache;
		if(stat != null)return stat;
		stat = new ConsumerGroupStat();
		stat.setGroupName(groupName);
		stat.setActived(zkHelper.consumerIsActive(groupName, consumerId));
		
		for (TopicStat topic : topicStats) {
			List<TopicPartitionStat> topicPartitionStats = zkHelper.getTopicOffsets(groupName, topic.getTopicName());
			for (TopicPartitionStat topicPartitionStat : topicPartitionStats) {
				kafkaHelper.getTopicPartitionLogSize(topicPartitionStat);
			}
			if(topicPartitionStats.size() > 0){
				topic.setPartitions(topicPartitionStats.size());
				topic.setPartitionStats(topicPartitionStats);
				stat.setTopicStats(topicStats);
			}
		}
		statCache = stat;
		return stat;
	}
	
	/**
	 *  kafka生产者统计
	 * @return [topic -> [successCount,errorCount]]
	 */
	public Map<String, Long[]> producerStats(){
		Map<String, Long[]> result = new HashMap<>();
		producerStats.forEach((k,v)->{
			result.put(k, new Long[]{v[0].get(),v[1].get()});
		});
		return result;
	}
	
	
	/**
	 * kafka状态统计
	 * @param consumerGroup
	 * @param latThreshold
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Map stats(String consumerGroup){
		if(latThreshold <= 0)latThreshold = 1000;
		Map<String, Object> map = new TreeMap<>();
		if(!producerStats.isEmpty())map.put("producerStats", producerStats());
		if(topicStats != null && !topicStats.isEmpty()){
			ConsumerGroupStat consumerStats = consumerStats(consumerGroup);
			//分析延时
			consumerStats.analysisLatThresholdStat(latThreshold);
			map.put("consumerStats", consumerStats);
		}
		
		return map;
	}
	
	/**
	 * 预警状态检查
	 * @param consumerGroup
	 * @return
	 */
	public Map<String, Object> healthStats(String consumerGroup){
		Map<String, Object> map = new TreeMap<>();
	
		Map<String, Long> producerStat = new TreeMap<>();
		producerStats.forEach((k,v)->{
			if(v[1].get() > 1){
				producerStat.put(k, v[1].get());
			}
		});
		if(!producerStat.isEmpty()){
			map.put("producerErrors", producerStat);
		}
		
		if(topicStats != null && !topicStats.isEmpty()){
			ConsumerGroupStat consumerStats = consumerStats(consumerGroup);
			//分析延时
			consumerStats.analysisLatThresholdStat(latThreshold);
			
			map.put("consumerStatus", consumerStats.isActived());
			if(consumerStats.isActived()){
				Map<String, Long> latTopics = new TreeMap<>();
				for (TopicStat topicStat : consumerStats.getTopicStats()) {
					if(topicStat.getTotalLat() > latThreshold){
						latTopics.put(topicStat.getTopicName(), topicStat.getTotalLat());
					}
				}
				if(!latTopics.isEmpty()){
					map.put("consumerLats", latTopics);
				}
			}
		}
		
		return map;
	}

	
}
