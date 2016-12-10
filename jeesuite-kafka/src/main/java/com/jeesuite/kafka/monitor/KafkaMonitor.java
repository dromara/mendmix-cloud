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

import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.Validate;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.kafka.monitor.model.BrokerInfo;
import com.jeesuite.kafka.monitor.model.ConsumerGroupInfo;
import com.jeesuite.kafka.monitor.model.ProducerInfo;
import com.jeesuite.kafka.monitor.model.ProducerStat;
import com.jeesuite.kafka.monitor.model.ProducerTopicInfo;
import com.jeesuite.kafka.producer.handler.SendCounterHandler;

import kafka.utils.ZKStringSerializer$;

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
	
	private List<ProducerInfo> producerStats = new ArrayList<>();
	
	private List<ConsumerGroupInfo> consumerGroupResult = new ArrayList<>();
	//
	private ScheduledExecutorService statScheduler;
	
	Lock lock = new ReentrantLock();// 锁 
	
	private ZkClient zkClient;


	public KafkaMonitor(String zkServers,String kafkaServers,int latThreshold) {
		Validate.notBlank(zkServers);
		Validate.notBlank(kafkaServers);
		this.latThreshold = latThreshold;
		
		zkClient = new ZkClient(zkServers, 10000, 10000, ZKStringSerializer$.MODULE$);
		
		try {			
			zkConsumerCommand = new ZkConsumerCommand(zkClient,zkServers, kafkaServers);
			kafkaConsumerCommand = new KafkaConsumerCommand(kafkaServers);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
					fetchProducerStatFromZK();
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
		try {zkClient.close();} catch (Exception e) {}
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

	public List<ProducerInfo> getProducerStats() {
		if(producerStats.isEmpty()){
			fetchProducerStatFromZK();
		}
		return producerStats;
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

	
	private synchronized void fetchProducerStatFromZK(){
		List<ProducerInfo> currentStats = new ArrayList<>();
		List<String> groups = zkClient.getChildren(SendCounterHandler.ROOT);
		if(groups == null)return ;
		
		List<String> nodes;
		ProducerInfo producerInfo;
		String groupPath,topicPath,nodePath;
		for (String group : groups) {
			producerInfo = new ProducerInfo(group);
			groupPath = SendCounterHandler.ROOT + "/" + group;
			List<String> topics = zkClient.getChildren(groupPath);
			for (String topic : topics) {				
				ProducerTopicInfo topicInfo = new ProducerTopicInfo(topic);
				topicPath = groupPath + "/" + topic;
				nodes = zkClient.getChildren(topicPath);
				for (String node : nodes) {
					nodePath = topicPath + "/" + node;
					Object data = zkClient.readData(nodePath);
					if(data != null){
						ProducerStat stat = JsonUtils.toObject(data.toString(), ProducerStat.class);
						stat.setSource(node);
						topicInfo.getProducerStats().add(stat);
					}
				}
				if(!topicInfo.getProducerStats().isEmpty()){
					producerInfo.getProducerTopics().add(topicInfo);
				}
			}
			if(!producerInfo.getProducerTopics().isEmpty()){
				currentStats.add(producerInfo);
			}
		}
		producerStats = currentStats;
	}

	
	public static void main(String[] args) {
		KafkaMonitor monitor = new KafkaMonitor("127.0.0.1:2181", "127.0.0.1:9092", 1000);
		
		//List<ConsumerGroupInfo> groupInfos = monitor.getAllConsumerGroupInfos();
		
		List<ProducerInfo> stats = monitor.getProducerStats();
		System.out.println(JsonUtils.toJson(stats));
		monitor.close();
	}
	
}
