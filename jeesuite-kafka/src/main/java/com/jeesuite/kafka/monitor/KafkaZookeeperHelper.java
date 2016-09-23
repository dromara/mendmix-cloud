/**
 * 
 */
package com.jeesuite.kafka.monitor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.common.requests.MetadataResponse.TopicMetadata;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.util.ResourceUtils;

import kafka.admin.AdminUtils;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class KafkaZookeeperHelper {

	private final static Logger logger = LoggerFactory.getLogger(KafkaZookeeperHelper.class);


	private ZkClient zkClient;
	private ZkUtils zkUtils;


	public KafkaZookeeperHelper() {
		if (zkClient != null)
			return;
		int sessionTimeoutMs = 5 * 1000;
		int connectionTimeoutMs = 3 * 1000;

		String zkServers = ResourceUtils.get("kafka.zkServers");
		zkClient = new ZkClient(zkServers, sessionTimeoutMs, connectionTimeoutMs, ZKStringSerializer$.MODULE$);
		
		boolean isSecureKafkaCluster = false;
		zkUtils = new ZkUtils(zkClient, new ZkConnection(zkServers), isSecureKafkaCluster);
	}
	
	public TopicMetadata getTopicMetadata(String topic) {
		TopicMetadata topicMetadata = AdminUtils.fetchTopicMetadataFromZk(topic, zkUtils);
		return topicMetadata;
	}
	
	public int getPartitionCounts(String topic){
		return getTopicMetadata(topic).partitionMetadata().size();
	}
	
	public List<String> getConsumers(String groupId){
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
	
	public List<TopicPartitionStat> getTopicOffsets(String groupId,String topic){
		List<TopicPartitionStat> result = new ArrayList<>();
		String path = "/consumers/" + groupId + "/offsets/"+topic;
		if(!zkClient.exists(path))return new ArrayList<>();
        List<String> children = zkClient.getChildren(path);
		
        TopicPartitionStat tp;
		for (String child : children) {
			Stat stat = new Stat();
			Object data = zkClient.readData(path + "/" + child,stat);
			tp = new TopicPartitionStat(topic, Integer.parseInt(child), Long.parseLong(data.toString()));
			tp.setCreateTime(new Date(stat.getCtime()));
			tp.setLastTime(new Date(stat.getMtime()));
			result.add(tp);
		}
		return result;
	}
	
	public boolean consumerIsActive(String groupId,String consumerId){
		return getConsumers(groupId).contains(consumerId);
	}
	
	public void close(){
		if(zkUtils != null){
			zkUtils.close();
			zkUtils = null;
			zkClient = null;
		}
	}
}
