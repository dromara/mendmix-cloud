/**
 * 
 */
package com.jeesuite.kafka.monitor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.protocol.SecurityProtocol;
import org.apache.kafka.common.requests.MetadataResponse.TopicMetadata;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.kafka.monitor.model.BrokerInfo;
import com.jeesuite.kafka.monitor.model.ConsumerGroupInfo;
import com.jeesuite.kafka.monitor.model.TopicPartitionInfo;

import kafka.admin.AdminUtils;
import kafka.cluster.Broker;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import scala.collection.Iterator;
import scala.collection.Seq;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class KafkaZookeeperHelper {

	private final static Logger logger = LoggerFactory.getLogger(KafkaZookeeperHelper.class);

	private static KafkaZookeeperHelper instance = new KafkaZookeeperHelper();

	public static KafkaZookeeperHelper getInstance() {
		return instance;
	}

	private ZkClient zkClient;
	private ZkUtils zkUtils;


	private KafkaZookeeperHelper() {
		if (zkClient != null)
			return;
		int sessionTimeoutMs = 5 * 1000;
		int connectionTimeoutMs = 3 * 1000;

		String zkServers = ResourceUtils.get("kafka.zkServers","127.0.0.1:2181");
		zkClient = new ZkClient(zkServers, sessionTimeoutMs, connectionTimeoutMs, ZKStringSerializer$.MODULE$);
		
		boolean isSecureKafkaCluster = false;
		zkUtils = new ZkUtils(zkClient, new ZkConnection(zkServers), isSecureKafkaCluster);
	}
	
	public List<BrokerInfo> fetchAllBrokers(){
		List<BrokerInfo> result = new ArrayList<>();
		Seq<Broker> brokers = zkUtils.getAllBrokersInCluster();
		Iterator<Broker> iterator = brokers.toIterator();
		while(iterator.hasNext()){
			Broker broker = iterator.next();
			Node node = broker.getNode(SecurityProtocol.PLAINTEXT);
			result.add(new BrokerInfo(node.idString(), node.host(), node.port()));
		}
		return result;
	}
	
	public List<ConsumerGroupInfo> fetchAllConsumerGroups(){
		List<ConsumerGroupInfo> result = new ArrayList<>();
		List<String> consumers = zkClient.getChildren("/consumers");
		if(consumers == null)return result;
		ConsumerGroupInfo consumerGroup;
		for (String  groupName : consumers) {
			consumerGroup = new ConsumerGroupInfo();
			consumerGroup.setGroupName(groupName);
			result.add(consumerGroup);
		}
		return result;
	}
	
	public TopicMetadata getTopicMetadata(String topic) {
		TopicMetadata topicMetadata = AdminUtils.fetchTopicMetadataFromZk(topic, zkUtils);
		return topicMetadata;
	}
	
	public int getPartitionCounts(String topic){
		return getTopicMetadata(topic).partitionMetadata().size();
	}
	
	public List<String> getConsumerClusterNodes(String groupId){
		String path = "/consumers/" + groupId + "/ids";
		if(zkClient.exists(path)){
			return zkClient.getChildren(path);
		}
		return new ArrayList<>();
	}
	
	public List<String> getSubscribeTopics(String groupId){
		String path = "/consumers/" + groupId + "/owners";
		if(zkClient.exists(path)){
			return zkClient.getChildren(path);
		}
		return new ArrayList<>();
	}
	
	public List<TopicPartitionInfo> getTopicOffsets(String groupId,String topic){
		List<TopicPartitionInfo> result = new ArrayList<>();
		String path = "/consumers/" + groupId + "/offsets/"+topic;
		if(!zkClient.exists(path))return new ArrayList<>();
        List<String> children = zkClient.getChildren(path);
		
        TopicPartitionInfo tp;
		for (String child : children) {
			Stat stat = new Stat();
			Object data = zkClient.readData(path + "/" + child,stat);
			tp = new TopicPartitionInfo(topic, Integer.parseInt(child), Long.parseLong(data.toString()));
			tp.setCreateTime(new Date(stat.getCtime()));
			tp.setLastTime(new Date(stat.getMtime()));
			result.add(tp);
		}
		return result;
	}
	
	public String fetchPartitionOwner(String groupId,String topic,int partition){
		String path = "/consumers/" + groupId + "/owners/"+topic + "/" + partition;
		try {			
			String value = zkClient.readData(path);
			return value;
		} catch (Exception e) {
			return null;
		}
	}
	
	public boolean consumerIsActive(String groupId,String consumerId){
		return getConsumerClusterNodes(groupId).contains(consumerId);
	}
	
	public void close(){
		if(zkUtils != null){
			zkUtils.close();
			zkUtils = null;
			zkClient = null;
		}
	}
}
