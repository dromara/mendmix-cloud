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
@ConfigurationProperties(prefix="jeesuite.kafka.consumer")
public class KafkaConsumerProperties implements EnvironmentAware{

	private boolean  independent;
	private boolean  useNewAPI;
	private int processThreads = 100;
	private String scanPackages;
	private Properties configs = new Properties();
	
	public boolean isIndependent() {
		return independent;
	}

	public void setIndependent(boolean independent) {
		this.independent = independent;
	}

	public boolean isUseNewAPI() {
		return useNewAPI;
	}

	public void setUseNewAPI(boolean useNewAPI) {
		this.useNewAPI = useNewAPI;
	}

	public int getProcessThreads() {
		return processThreads;
	}

	public void setProcessThreads(int processThreads) {
		this.processThreads = processThreads;
	}

	public String getScanPackages() {
		return scanPackages;
	}

	public void setScanPackages(String scanPackages) {
		this.scanPackages = scanPackages;
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
		String zkServers = environment.getProperty("kafka.zkServers");
		configs.put("bootstrap.servers", kafkaServers);
		if(useNewAPI == false && zkServers != null){
			configs.put("zookeeper.connect", zkServers);
		}
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment, "kafka.consumer.");
		Map<String, Object> subProperties = resolver.getSubProperties("");
		if(subProperties != null && !subProperties.isEmpty()){
			configs.putAll(subProperties);
		}
	}
	
}
