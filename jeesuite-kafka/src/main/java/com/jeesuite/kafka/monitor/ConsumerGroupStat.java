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
public class ConsumerGroupStat {

	private String groupName;
	
	private boolean actived;
	
	private int restartCount;
	
	private List<TopicStat> topicStats;
	
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

	public int getRestartCount() {
		return restartCount;
	}

	public void setRestartCount(int restartCount) {
		this.restartCount = restartCount;
	}

	public List<TopicStat> getTopicStats() {
		return topicStats;
	}

	public void setTopicStats(List<TopicStat> topicStats) {
		this.topicStats = topicStats;
	}
	
	public boolean isOverLatThreshold() {
		return overLatThreshold;
	}

	public void setOverLatThreshold(boolean overLatThreshold) {
		this.overLatThreshold = overLatThreshold;
	}

	public void analysisLatThresholdStat(long latThreshold){
		for (TopicStat topicStat : topicStats) {
			if(topicStat.getPartitionStats() == null)continue;
			long totalLats = 0;
			for (TopicPartitionStat partitionStat : topicStat.getPartitionStats()) {
				totalLats += partitionStat.getLat();
			}
			boolean overLatThreshold = totalLats > latThreshold;
			if(overLatThreshold)this.overLatThreshold = true;
			topicStat.setOverLatThreshold(overLatThreshold);
		}
	}

}
