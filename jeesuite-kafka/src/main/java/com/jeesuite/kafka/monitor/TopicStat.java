/**
 * 
 */
package com.jeesuite.kafka.monitor;

import java.util.List;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月22日
 */
public class TopicStat {

	private String topicName;

	private int partitions; // 分片数


	List<TopicPartitionStat> partitionStats;
	
	private boolean overLatThreshold;
	
	private long totalLogSize;
	
	private long totalOffset;

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public int getPartitions() {
		return partitions;
	}

	public void setPartitions(int partitions) {
		this.partitions = partitions;
	}

	public List<TopicPartitionStat> getPartitionStats() {
		return partitionStats;
	}

	public void setPartitionStats(List<TopicPartitionStat> partitionStats) {
		this.partitionStats = partitionStats;
	}

	public boolean isOverLatThreshold() {
		return overLatThreshold;
	}

	public void setOverLatThreshold(boolean overLatThreshold) {
		this.overLatThreshold = overLatThreshold;
	}
	
	
	public long getTotalLogSize() {
		if(totalLogSize > 0 || partitionStats == null)return totalLogSize;
		for (TopicPartitionStat tp : partitionStats) {
			totalLogSize+=tp.getLogSize();
		}
		return totalLogSize;
	}

	public long getTotalOffset() {
		if(totalOffset > 0 || partitionStats == null)return totalOffset;
		for (TopicPartitionStat tp : partitionStats) {
			totalOffset+=tp.getOffset();
		}
		return totalOffset;
	}
	
	public long getTotalLat(){
		return getTotalLogSize() - getTotalOffset();
	}
	
}
