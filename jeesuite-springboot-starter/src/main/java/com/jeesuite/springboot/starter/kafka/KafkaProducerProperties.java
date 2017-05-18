/**
 * 
 */
package com.jeesuite.springboot.starter.kafka;

import java.util.Map;
import java.util.Properties;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月31日
 */
@ConfigurationProperties(prefix="jeesuite.kafka.producer")
public class KafkaProducerProperties implements EnvironmentAware{

	
	private boolean  defaultAsynSend;
	private String producerGroup;
	private String monitorZkServers;
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
	
	public String getMonitorZkServers() {
		return monitorZkServers;
	}
	public void setMonitorZkServers(String monitorZkServers) {
		this.monitorZkServers = monitorZkServers;
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
	
	@Override
	public void setEnvironment(Environment environment) {
		String kafkaServers = environment.getProperty("kafka.bootstrap.servers");
		//String zkServers = environment.getProperty("zookeeper.servers");
		configs.put("bootstrap.servers", kafkaServers);
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment, "kafka.producer.");
		Map<String, Object> subProperties = resolver.getSubProperties("");
		if(subProperties != null && !subProperties.isEmpty()){
			configs.putAll(subProperties);
		}
	}
	
	
	
}
