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

import com.jeesuite.kafka.monitor.model.BrokerInfo;
import com.jeesuite.kafka.monitor.model.ConsumerGroupInfo;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class KafkaMonitor implements Closeable{

	private ZkConsumerCommand zkConsumerCommand;
	private KafkaConsumerCommand kafkaConsumerCommand;
	
	//消费延时阀值
	private int latThreshold = 2000;
	
	private List<ConsumerGroupInfo> consumerGroupResult = new ArrayList<>();
	private int currentStatHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); //当前统计的小时
	//<topic,[success,error]>
	private Map<String, AtomicLong[]> producerStats = new HashMap<>();
	
	//
	private ScheduledExecutorService statScheduler;
	
	Lock lock = new ReentrantLock();// 锁 


	private KafkaMonitor(String zkServers,String kafkaServers,int latThreshold) {
		this.latThreshold = latThreshold;
		zkConsumerCommand = new ZkConsumerCommand(zkServers, kafkaServers);
		kafkaConsumerCommand = new KafkaConsumerCommand(kafkaServers);
		// 
		initCollectionTimer();
	}

	/**
	 * 
	 */
	private void initCollectionTimer() {
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
					//抓取kafka消费组信息
					fetchConsumerGroupFromKafkaAndZK();
				} finally {
					lock.unlock();
				}
			}
		}, 1, 5, TimeUnit.SECONDS);
	}
	
	public void close(){
		statScheduler.shutdown();
		kafkaConsumerCommand.close();
		zkConsumerCommand.close();
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
		return zkConsumerCommand.fetchAllBrokers();
	}
	
	public List<ConsumerGroupInfo> getAllConsumerGroupInfos(){
		if(consumerGroupResult.isEmpty()){
			fetchConsumerGroupFromKafkaAndZK();
		}
		return consumerGroupResult;
	}

	/**
	 * @return
	 */
	private synchronized void fetchConsumerGroupFromKafkaAndZK() {
		consumerGroupResult = kafkaConsumerCommand.getAllConsumerGroups();
		List<ConsumerGroupInfo> list2 = zkConsumerCommand.getAllConsumerGroups();
		
		if(list2 != null && list2.size() > 0){
			consumerGroupResult.addAll(list2);
		}
		for (ConsumerGroupInfo consumer : list2) {
			consumer.analysisLatThresholdStat(latThreshold);
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
		
	}
	
}
