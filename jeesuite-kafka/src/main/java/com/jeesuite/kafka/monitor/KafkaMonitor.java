/**
 * 
 */
package com.jeesuite.kafka.monitor;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.Validate;

import com.jeesuite.common.json.JsonUtils;
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
	//
	private ScheduledExecutorService statScheduler;
	
	Lock lock = new ReentrantLock();// 锁 


	public KafkaMonitor(String zkServers,String kafkaServers,int latThreshold) {
		Validate.notBlank(zkServers);
		Validate.notBlank(kafkaServers);
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
				try {
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
		
		if(list2 != null){
			for (ConsumerGroupInfo consumer : list2) {
				if(consumer.getTopics().isEmpty())continue;
				consumerGroupResult.add(consumer);
			}
		}
		for (ConsumerGroupInfo consumer : consumerGroupResult) {
			consumer.analysisLatThresholdStat(latThreshold);
		}
	}

	

	
	public static void main(String[] args) {
		KafkaMonitor monitor = new KafkaMonitor("127.0.0.1:2181", "127.0.0.1:9092", 1000);
		
		List<ConsumerGroupInfo> groupInfos = monitor.getAllConsumerGroupInfos();
		
		System.out.println(JsonUtils.toJson(groupInfos));
		monitor.close();
	}
	
}
