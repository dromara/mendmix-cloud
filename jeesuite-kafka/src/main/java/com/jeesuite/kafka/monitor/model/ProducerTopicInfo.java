/**
 * 
 */
package com.jeesuite.kafka.monitor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月10日
 */
public class ProducerTopicInfo {

	private String topic;
	private List<ProducerStat> producerStats = new ArrayList<>();
	
	public ProducerTopicInfo() {}
	
	public ProducerTopicInfo(String topic) {
		super();
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public List<ProducerStat> getProducerStats() {
		return producerStats;
	}
	public void setProducerStats(List<ProducerStat> producerStats) {
		this.producerStats = producerStats;
	}
	
	
}
