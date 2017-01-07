/**
 * 
 */
package com.jeesuite.kafka.monitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.protocol.SecurityProtocol;
import org.apache.kafka.common.requests.MetadataResponse.TopicMetadata;
import org.apache.zookeeper.data.Stat;

import com.jeesuite.kafka.monitor.model.BrokerInfo;
import com.jeesuite.kafka.monitor.model.ConsumerGroupInfo;
import com.jeesuite.kafka.monitor.model.TopicInfo;
import com.jeesuite.kafka.monitor.model.TopicPartitionInfo;

import kafka.admin.AdminUtils;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.cluster.Broker;
import kafka.cluster.BrokerEndPoint;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import scala.collection.Iterator;
import scala.collection.Seq;

/**
 * 消费者信息获取命令工具（旧版API）
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月9日
 */
public class ZkConsumerCommand {
	
	private static final String CLIENT_ID = "ConsumerOffsetChecker";
	private List<String> kafkaServerList = new ArrayList<String>();
	
	private Map<String, SimpleConsumer> consumers = new ConcurrentHashMap<>();
	

	private ZkClient zkClient;
	private ZkUtils zkUtils;
	
	public ZkConsumerCommand(ZkClient zkClient,String zkServers,String kafkaServers) {
		
		kafkaServerList.addAll(Arrays.asList(kafkaServers.split(",")));
		
		if(zkClient == null){			
			zkClient = new ZkClient(zkServers, 10000, 10000, ZKStringSerializer$.MODULE$);
		}
		this.zkClient = zkClient;
		
		boolean isSecureKafkaCluster = false;
		zkUtils = new ZkUtils(zkClient, new ZkConnection(zkServers), isSecureKafkaCluster);
	}

	public List<ConsumerGroupInfo> getAllConsumerGroups(){
		List<ConsumerGroupInfo> consumerGroups = fetchAllConsumerGroups();
		for (ConsumerGroupInfo consumerGroup : consumerGroups) {
			loadTopicInfoInConsumerGroup(consumerGroup);
		}
		return consumerGroups;
	}
	
	private void loadTopicInfoInConsumerGroup(ConsumerGroupInfo group){
		List<String> subscribeTopics = getSubscribeTopics(group.getGroupName());
		for (String topic : subscribeTopics) {
			TopicInfo topicInfo = new TopicInfo();
			topicInfo.setTopicName(topic);
			List<TopicPartitionInfo> topicPartitions = getTopicOffsets(group.getGroupName(), topic);
			
			boolean allPartNotOwner = true;
			for (TopicPartitionInfo partition : topicPartitions) {
				getTopicPartitionLogSize(partition);
				//owner
				String owner = fetchPartitionOwner(group.getGroupName(), topic, partition.getPartition());
				if(owner != null){
					allPartNotOwner = false;
					partition.setOwner(owner);
					if(!group.isActived()){
						group.setActived(true);
					}
				}
			}
			//如果所有分区都没有消费者线程就不显示
			if(allPartNotOwner == false && topicPartitions.size() > 0){
				topicInfo.setPartitions(topicPartitions);
				group.getTopics().add(topicInfo);
			}
		}
	}
	
	public List<BrokerInfo> fetchAllBrokers(){
		List<BrokerInfo> result = new ArrayList<>();
		Seq<Broker> brokers = zkUtils.getAllBrokersInCluster();
		Iterator<Broker> iterator = brokers.toList().iterator();
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
			List<String> consumerClusterNodes = getConsumerClusterNodes(groupName);
			if(consumerClusterNodes == null || consumerClusterNodes.isEmpty()){
				continue;
			}
			consumerGroup = new ConsumerGroupInfo();
			consumerGroup.setClusterNodes(consumerClusterNodes);
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
	
	private SimpleConsumer getConsumerClient(String kafkaServer){
    	if(consumers.containsKey(kafkaServer))return consumers.get(kafkaServer);
    	String host = kafkaServer.split(":")[0];
		int port = Integer.parseInt(kafkaServer.split(":")[1]);
		
		SimpleConsumer consumer = new SimpleConsumer(host, port, 100000, 64 * 1024, CLIENT_ID);
		consumers.put(kafkaServer, consumer);
		return consumer;
    }
    
    private SimpleConsumer getConsumerClient(String host,int port){
    	return getConsumerClient(host + ":" + port);
    }
    
    /**
     * 获取指定主题及分区logsize
     * @param stat
     */
    public void getTopicPartitionLogSize(TopicPartitionInfo stat){
    	BrokerEndPoint leader = findLeader(stat.getTopic(), stat.getPartition()).leader();
    	SimpleConsumer consumer = getConsumerClient(leader.host(), leader.port());	
    	
    	try {			
    		long logsize = getLastOffset(consumer,stat.getTopic(), stat.getPartition(), kafka.api.OffsetRequest.LatestTime());
    		stat.setLogSize(logsize);
		} finally {
			consumer.close();
		}
    }

	private static long getLastOffset(SimpleConsumer consumer, String topic, int partition, long whichTime) {
		TopicAndPartition topicAndPartition = new TopicAndPartition(topic, partition);
		Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
		requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(whichTime, 1));
		kafka.javaapi.OffsetRequest request = new kafka.javaapi.OffsetRequest(requestInfo,
				kafka.api.OffsetRequest.CurrentVersion(), CLIENT_ID);
		OffsetResponse response = consumer.getOffsetsBefore(request);

		if (response.hasError()) {
			System.out.println(
					"Error fetching data Offset Data the Broker. Reason: " + response.errorCode(topic, partition));
			return 0;
		}
		long[] offsets = response.offsets(topic, partition);
		return offsets[0];
	}

	private PartitionMetadata findLeader(String a_topic, int a_partition) {
		PartitionMetadata returnMetaData = null;
		loop: for (String kafkaServer : kafkaServerList) {
			SimpleConsumer consumer = null;

			String host = kafkaServer.split(":")[0];
			int port = Integer.parseInt(kafkaServer.split(":")[1]);
			try {
				consumer = new SimpleConsumer(host, port, 100000, 64 * 1024, "leaderLookup");
				List<String> topics = Collections.singletonList(a_topic);
				TopicMetadataRequest req = new TopicMetadataRequest(topics);
				kafka.javaapi.TopicMetadataResponse resp = consumer.send(req);

				List<kafka.javaapi.TopicMetadata> metaData = resp.topicsMetadata();
				for (kafka.javaapi.TopicMetadata item : metaData) {
					for (PartitionMetadata part : item.partitionsMetadata()) {
						if (part.partitionId() == a_partition) {
							returnMetaData = part;
							break loop;
						}
					}
				}
			} catch (Exception e) {
				System.out.println("Error communicating with Broker [" + kafkaServer + "] to find Leader for ["
						+ a_topic + ", " + a_partition + "] Reason: " + e);
			} finally {
				if (consumer != null)
					consumer.close();
			}
		}
		if (returnMetaData != null) {
			kafkaServerList.clear();
			for (kafka.cluster.BrokerEndPoint replica : returnMetaData.replicas()) {
				kafkaServerList.add(replica.host() + ":" + replica.port());
			}
		}
		return returnMetaData;
	}
	
	public void close(){
		if(zkUtils != null){
			zkUtils.close();
			zkUtils = null;
			zkClient = null;
		}
	}
}
