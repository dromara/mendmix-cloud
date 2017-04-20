/**
 * 
 */
package com.jeesuite.springboot.starter.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jeesuite.cache.redis.JedisProviderFactoryBean;

import redis.clients.jedis.JedisPoolConfig;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年3月28日
 */
@Configuration
@EnableConfigurationProperties(KafkaProducerProperties.class)
public class DelegateKafkaProducerConfiguration {

	@Autowired
	private KafkaProducerProperties producerProperties;

	
	@Bean
	public JedisProviderFactoryBean jedisPool() {

		JedisProviderFactoryBean bean = new JedisProviderFactoryBean();

		System.out.println(producerProperties);
		return bean;
	}

	
	
}
