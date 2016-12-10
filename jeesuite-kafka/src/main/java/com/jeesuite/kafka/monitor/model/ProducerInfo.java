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
public class ProducerInfo {

	private String name;
	
	private List<ProducerTopicInfo> producerTopics;
	
	public ProducerInfo() {}

	public ProducerInfo(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ProducerTopicInfo> getProducerTopics() {
		return producerTopics == null ? (producerTopics = new ArrayList<>()) : producerTopics;
	}

	public void setProducerTopics(List<ProducerTopicInfo> producerTopics) {
		this.producerTopics = producerTopics;
	}
	
	
}
