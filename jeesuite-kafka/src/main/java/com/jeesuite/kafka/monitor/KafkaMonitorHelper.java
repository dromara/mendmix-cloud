/**
 * 
 */
package com.jeesuite.kafka.monitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jeesuite.common.util.ResourceUtils;

import kafka.api.PartitionOffsetRequestInfo;
import kafka.cluster.BrokerEndPoint;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.consumer.SimpleConsumer;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class KafkaMonitorHelper {

	private static final String CLIENT_ID = "ConsumerOffsetChecker";
	private List<String> kafkaServers = new ArrayList<String>();
	
	private Map<String, SimpleConsumer> consumers = new ConcurrentHashMap<>();
	
	
	public KafkaMonitorHelper() {
		String servers = ResourceUtils.get("kafka.servers");	
		kafkaServers.addAll(Arrays.asList(servers.split(",")));
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
    public void getTopicPartitionLogSize(TopicPartitionStat stat){
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
		loop: for (String kafkaServer : kafkaServers) {
			SimpleConsumer consumer = null;

			String host = kafkaServer.split(":")[0];
			int port = Integer.parseInt(kafkaServer.split(":")[1]);
			try {
				consumer = new SimpleConsumer(host, port, 100000, 64 * 1024, "leaderLookup");
				List<String> topics = Collections.singletonList(a_topic);
				TopicMetadataRequest req = new TopicMetadataRequest(topics);
				kafka.javaapi.TopicMetadataResponse resp = consumer.send(req);

				List<TopicMetadata> metaData = resp.topicsMetadata();
				for (TopicMetadata item : metaData) {
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
			kafkaServers.clear();
			for (kafka.cluster.BrokerEndPoint replica : returnMetaData.replicas()) {
				kafkaServers.add(replica.host() + ":" + replica.port());
			}
		}
		return returnMetaData;
	}
}
