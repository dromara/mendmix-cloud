/**
 * 
 */
package com.jeesuite.kafka.monitor.model;

import java.util.List;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class TopicInfo {

	private String topicName;

	private int partitionNums; // 分片数


	List<TopicPartitionInfo> partitions;
	
	private boolean overLatThreshold;
	
	private long totalLogSize;
	
	private long totalOffset;

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public int getPartitionNums() {
		return partitionNums;
	}

	public void setPartitionNums(int partitions) {
		this.partitionNums = partitions;
	}

	public List<TopicPartitionInfo> getPartitions() {
		return partitions;
	}

	public void setPartitions(List<TopicPartitionInfo> partitions) {
		this.partitions = partitions;
	}

	public boolean isOverLatThreshold() {
		return overLatThreshold;
	}

	public void setOverLatThreshold(boolean overLatThreshold) {
		this.overLatThreshold = overLatThreshold;
	}
	
	
	public long getTotalLogSize() {
		if(totalLogSize > 0 || partitions == null)return totalLogSize;
		for (TopicPartitionInfo tp : partitions) {
			totalLogSize+=tp.getLogSize();
		}
		return totalLogSize;
	}

	public long getTotalOffset() {
		if(totalOffset > 0 || partitions == null)return totalOffset;
		for (TopicPartitionInfo tp : partitions) {
			totalOffset+=tp.getOffset();
		}
		return totalOffset;
	}
	
	public long getTotalLat(){
		return getTotalLogSize() - getTotalOffset();
	}
	
}
