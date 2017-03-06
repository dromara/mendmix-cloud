/**
 * 
 */
package com.jeesuite.kafka.monitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.kafka.monitor.model.ConsumerGroupInfo;
import com.jeesuite.kafka.monitor.model.TopicInfo;
import com.jeesuite.kafka.monitor.model.TopicPartitionInfo;

import kafka.admin.AdminClient;
import kafka.admin.AdminClient.ConsumerSummary;
import kafka.coordinator.GroupOverview;
import scala.collection.Iterator;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月8日
 */
public class KafkaConsumerCommand {

	private Map<String, KafkaConsumer<String, Serializable>> kafkaConsumers = new HashMap<>();
	private AdminClient adminClient;
	private String bootstrapServer;

	public KafkaConsumerCommand(String bootstrapServer) {
		this.bootstrapServer = bootstrapServer;
		Properties props = new Properties();
		props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
		adminClient = AdminClient.create(props);
	}
	
	public List<ConsumerGroupInfo> getAllConsumerGroups(){
		List<ConsumerGroupInfo> consumerGroups = new ArrayList<>();
		List<String> groupIds = group();
		for (String groupId : groupIds) {
			consumerGroups.add(consumerGroup(groupId));
		}
		return consumerGroups;
	}

	protected List<String> group() {
		List<String> groups = new ArrayList<>();
		scala.collection.immutable.List<GroupOverview> list = adminClient.listAllConsumerGroupsFlattened();
		if (list == null)
			return groups;
		Iterator<GroupOverview> iterator = list.iterator();
		while (iterator.hasNext()) {
			groups.add(iterator.next().groupId());
		}
		return groups;
	}
	
	public ConsumerGroupInfo consumerGroup(String group){
		KafkaConsumer<String, Serializable> kafkaConsumer = getConsumer(group);
		return consumerGroup(kafkaConsumer,group);
	}
	
	public ConsumerGroupInfo consumerGroup(KafkaConsumer<String, Serializable> kafkaConsumer,String group){
		
		scala.collection.immutable.List<ConsumerSummary> consumers = adminClient.describeConsumerGroup(group);
		if(consumers.isEmpty()){
			System.out.println("Consumer group ["+group+"] does not exist or is rebalancing.");
			return null;
		}
		
		ConsumerGroupInfo consumerGroup = new ConsumerGroupInfo();
		consumerGroup.setActived(true);
		consumerGroup.setGroupName(group);
		Iterator<ConsumerSummary> iterator = consumers.iterator();
		while (iterator.hasNext()) {
			ConsumerSummary consumer = iterator.next();
			if(!consumerGroup.getClusterNodes().contains(consumer.clientId())){
				consumerGroup.getClusterNodes().add(consumer.clientId());
			}
			String owner = consumer.clientId() + consumer.clientHost();
			scala.collection.immutable.List<TopicPartition> partitions = consumer.assignment();
			Iterator<TopicPartition> iterator2 = partitions.iterator();
			
			Map<String, TopicInfo> topicInfos = new HashMap<>();
			while (iterator2.hasNext()) {
				TopicPartition partition = iterator2.next();
				
				TopicInfo topicInfo = topicInfos.get(partition.topic());
				if(topicInfo == null){					
					topicInfos.put(partition.topic(), topicInfo = new TopicInfo(partition.topic()));
					consumerGroup.getTopics().add(topicInfo);
				}
				OffsetAndMetadata metadata = kafkaConsumer.committed(new TopicPartition(partition.topic(), partition.partition()));
				
				TopicPartitionInfo partitionInfo = new TopicPartitionInfo(partition.topic(), partition.partition(),metadata.offset(),owner);
				//
				long logSize = getLogSize(kafkaConsumer,partition.topic(), partition.partition());
				partitionInfo.setLogSize(logSize);
				topicInfo.getPartitions().add(partitionInfo);
			}
		}
		
		return consumerGroup;
		
	}
	
	public void resetTopicOffsets(String groupId,String topic,int partition,long newOffsets){
		KafkaConsumer<String, Serializable> kafkaConsumer = getConsumer(groupId);
		kafkaConsumer.seek(new TopicPartition(topic, partition), newOffsets);
	}

	protected long getLogSize(KafkaConsumer<String, Serializable> kafkaConsumer,String topic, int partition) {
		TopicPartition topicPartition = new TopicPartition(topic, partition);
		List<TopicPartition> asList = Arrays.asList(topicPartition);
		kafkaConsumer.assign(asList);
		kafkaConsumer.seekToEnd(asList);
		long logEndOffset = kafkaConsumer.position(topicPartition);
		return logEndOffset;
	}

	private KafkaConsumer<String, Serializable> getConsumer(String groupId) {
		
		KafkaConsumer<String, Serializable> kafkaConsumer = null; 
		if ((kafkaConsumer = kafkaConsumers.get(groupId))!= null)
			return kafkaConsumer;
		
		Properties properties = new Properties();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

		kafkaConsumer = new KafkaConsumer<String, Serializable>(properties);
		kafkaConsumers.put(groupId, kafkaConsumer);
		return kafkaConsumer;

	}

	public void close() {
		adminClient.close();
		if (kafkaConsumers != null){
			for (KafkaConsumer<String, Serializable> kafkaConsumer : kafkaConsumers.values()) {				
				kafkaConsumer.close();
			}
		}
	}
	
	public static void main(String[] args) {
		
		KafkaConsumerCommand command = new KafkaConsumerCommand("127.0.0.1:9092");
		
		System.out.println(command.group());
		
		List<ConsumerGroupInfo> list = command.getAllConsumerGroups();
		System.out.println(JsonUtils.toJson(list));
		
		command.close();
	}
}
