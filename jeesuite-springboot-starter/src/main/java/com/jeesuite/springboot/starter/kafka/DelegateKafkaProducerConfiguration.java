/**
 * 
 */
package com.jeesuite.springboot.starter.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jeesuite.kafka.spring.TopicProducerSpringProvider;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年3月28日
 */
@Configuration
@EnableConfigurationProperties(KafkaProducerProperties.class)
@ConditionalOnClass(TopicProducerSpringProvider.class)
public class DelegateKafkaProducerConfiguration {

	@Autowired
	private KafkaProducerProperties producerProperties;

	@Bean
	public TopicProducerSpringProvider producerProvider() {

		TopicProducerSpringProvider bean = new TopicProducerSpringProvider();
		bean.setConfigs(producerProperties.getConfigs());
		bean.setDefaultAsynSend(producerProperties.isDefaultAsynSend());
		bean.setDelayRetries(producerProperties.getDelayRetries());
		bean.setMonitorZkServers(producerProperties.getMonitorZkServers());
		bean.setProducerGroup(producerProperties.getProducerGroup());

		return bean;
	}

	
	
}
