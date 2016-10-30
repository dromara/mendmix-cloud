/**
 * 
 */
package com.jeesuite.kafka.monitor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class ConsumerGroupInfo {

	private String groupName;
	
	private boolean actived;
	
	private List<TopicInfo> topics;
	
	private List<String> clusterNodes;
	
	private boolean overLatThreshold;

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public boolean isActived() {
		return actived;
	}

	public void setActived(boolean actived) {
		this.actived = actived;
	}

	public List<TopicInfo> getTopics() {
		return topics == null ? (topics = new ArrayList<>()) : topics;
	}

	public void setTopics(List<TopicInfo> topics) {
		this.topics = topics;
	}

	public List<String> getClusterNodes() {
		return clusterNodes;
	}

	public void setClusterNodes(List<String> clusterNodes) {
		this.clusterNodes = clusterNodes;
	}

	public boolean isOverLatThreshold() {
		return overLatThreshold;
	}

	public void setOverLatThreshold(boolean overLatThreshold) {
		this.overLatThreshold = overLatThreshold;
	}

	public void analysisLatThresholdStat(long latThreshold){
		for (TopicInfo topicStat : getTopics()) {
			if(topicStat.getPartitions() == null)continue;
			long totalLats = 0;
			for (TopicPartitionInfo partitionStat : topicStat.getPartitions()) {
				totalLats += partitionStat.getLat();
			}
			boolean overLatThreshold = totalLats > latThreshold;
			if(overLatThreshold)this.overLatThreshold = true;
			topicStat.setOverLatThreshold(overLatThreshold);
		}
	}

}
