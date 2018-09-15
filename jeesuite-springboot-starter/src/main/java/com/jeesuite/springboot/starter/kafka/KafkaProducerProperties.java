/**
 * 
 */
package com.jeesuite.springboot.starter.kafka;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.jeesuite.common.util.ResourceUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月31日
 */
@ConfigurationProperties(prefix="jeesuite.kafka.producer")
public class KafkaProducerProperties implements InitializingBean{

	
	private boolean  defaultAsynSend;
	private String producerGroup;
	private boolean monitorEnabled;
	private boolean consumerAckEnabled;
	private int delayRetries = 0;
	private Properties configs = new Properties();
	
	
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
	
	public boolean isMonitorEnabled() {
		return monitorEnabled;
	}
	public void setMonitorEnabled(boolean monitorEnabled) {
		this.monitorEnabled = monitorEnabled;
	}
	public int getDelayRetries() {
		return delayRetries;
	}
	public void setDelayRetries(int delayRetries) {
		this.delayRetries = delayRetries;
	}
	public Properties getConfigs() {
		return configs;
	}
	public void setConfigs(Properties configs) {
		this.configs = configs;
	}
	
	public boolean isConsumerAckEnabled() {
		return consumerAckEnabled;
	}
	public void setConsumerAckEnabled(boolean consumerAckEnabled) {
		this.consumerAckEnabled = consumerAckEnabled;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		String kafkaServers = ResourceUtils.getProperty("kafka.bootstrap.servers");
		configs.put("bootstrap.servers", kafkaServers);
		Properties properties = ResourceUtils.getAllProperties("kafka.producer.");
		Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<Object, Object> entry = iterator.next();
			configs.put(entry.getKey().toString().replace("kafka.producer.", ""), entry.getValue());
		}
	}
	
	
}
