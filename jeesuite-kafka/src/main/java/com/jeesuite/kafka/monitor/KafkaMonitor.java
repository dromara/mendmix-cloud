/**
 * 
 */
package com.jeesuite.kafka.monitor;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.kafka.monitor.model.BrokerInfo;
import com.jeesuite.kafka.monitor.model.ConsumerGroupInfo;
import com.jeesuite.kafka.monitor.model.TopicInfo;
import com.jeesuite.kafka.monitor.model.TopicPartitionInfo;
import com.jeesuite.kafka.utils.KafkaConst;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class KafkaMonitor implements Closeable{

	private KafkaZookeeperHelper zkHelper = KafkaZookeeperHelper.getInstance();
	
	private KafkaMonitorHelper kafkaHelper = new KafkaMonitorHelper();

	private static KafkaMonitor context = new KafkaMonitor();;
	
	private List<TopicInfo> topicStats = new ArrayList<>();
	
	//消费延时阀值
	private int latThreshold = 2000;
	
	private int currentStatHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); //当前统计的小时
	//<topic,[success,error]>
	private Map<String, AtomicLong[]> producerStats = new HashMap<>();
	
	//
	private ScheduledExecutorService statScheduler;
	
	
	Lock lock = new ReentrantLock();// 锁 
	
	public static KafkaMonitor getContext() {
		return context;
	}

	private KafkaMonitor() {
		latThreshold = Integer.parseInt(ResourceUtils.get(KafkaConst.PROP_TOPIC_LAT_THRESHOLD, "100"));
		// 
		statScheduler = Executors.newScheduledThreadPool(1);
		statScheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				lock.lock();
				int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				try {
					//清除上一个小时的统计
					if(hourOfDay != currentStatHourOfDay){
						producerStats.clear();
					}
					//清除缓存
				} finally {
					lock.unlock();
				}
			}
		}, 1, 30, TimeUnit.SECONDS);
	}
	
	public void close(){
		statScheduler.shutdown();
		zkHelper.close();
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
	
	public List<BrokerInfo> getAllBrokers(){
		return zkHelper.fetchAllBrokers();
	}
	
	public List<ConsumerGroupInfo> getAllConsumerGroupInfos(){
		List<ConsumerGroupInfo> consumerGroups = zkHelper.fetchAllConsumerGroups();
		for (ConsumerGroupInfo consumerGroup : consumerGroups) {
			loadTopicInfoInConsumerGroup(consumerGroup);
			//分析延时
			consumerGroup.analysisLatThresholdStat(latThreshold);
			//
			consumerGroup.setClusterNodes(zkHelper.getConsumerClusterNodes(consumerGroup.getGroupName()));
		}
		return consumerGroups;
	}

	private void loadTopicInfoInConsumerGroup(ConsumerGroupInfo group){
		List<String> subscribeTopics = zkHelper.getSubscribeTopics(group.getGroupName());
		for (String topic : subscribeTopics) {
			TopicInfo topicInfo = new TopicInfo();
			topicInfo.setTopicName(topic);
			List<TopicPartitionInfo> topicPartitions = zkHelper.getTopicOffsets(group.getGroupName(), topic);
			for (TopicPartitionInfo partition : topicPartitions) {
				kafkaHelper.getTopicPartitionLogSize(partition);
				//owner
				String owner = zkHelper.fetchPartitionOwner(group.getGroupName(), topic, partition.getPartition());
				if(owner != null){
					partition.setOwner(owner);
					if(!group.isActived()){
						group.setActived(true);
					}
				}
			}
			if(topicPartitions.size() > 0){
				topicInfo.setPartitionNums(topicPartitions.size());
				topicInfo.setPartitions(topicPartitions);
				group.getTopics().add(topicInfo);
			}
		}
	}
	
	/**
	 *  kafka生产者统计
	 * @return [topic -> [successCount,errorCount]]
	 */
	public Map<String, Long[]> producerStats(){
		Map<String, Long[]> result = new HashMap<>();
//		producerStats.forEach((k,v)->{
//			result.put(k, new Long[]{v[0].get(),v[1].get()});
//		});
		Set<String> keys = producerStats.keySet();
		for (String key : keys) {
			AtomicLong[] v = producerStats.get(key);
			result.put(key, new Long[]{v[0].get(),v[1].get()});
		}
		return result;
	}

	
	public static void main(String[] args) {
		List<ConsumerGroupInfo> groupInfos = KafkaMonitor.getContext().getAllConsumerGroupInfos();
		for (ConsumerGroupInfo groupInfo : groupInfos) {
			System.out.println(JsonUtils.toJson(groupInfo));
		}
		KafkaMonitor.getContext().close();
	}
	
}
