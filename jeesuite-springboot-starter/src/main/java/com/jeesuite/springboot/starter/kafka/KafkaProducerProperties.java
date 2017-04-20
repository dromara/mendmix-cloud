/**
 * 
 */
package com.jeesuite.springboot.starter.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月31日
 */
@ConfigurationProperties(prefix="jeesuite.kafka.producer")
public class KafkaProducerProperties {

	
	private boolean  defaultAsynSend;
	private String producerGroup;
	
	public boolean isDefaultAsynSend() {
		return defaultAsynSend;
	}
	public void setDefaultAsynSend(boolean defaultAsynSend) {
		this.defaultAsynSend = defaultAsynSend;
	}
	public String getProducerGroup() {
		return producerGroup;
	}
	public void setProducerGroup(String producerGroup) {
		this.producerGroup = producerGroup;
	}
	
	
	
}
