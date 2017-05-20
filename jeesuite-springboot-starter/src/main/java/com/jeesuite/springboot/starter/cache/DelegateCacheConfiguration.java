/**
 * 
 */
package com.jeesuite.springboot.starter.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnClass(JedisProviderFactoryBean.class)
@ConditionalOnProperty(name="jeesuite.cache.mode")
public class DelegateCacheConfiguration {

	@Autowired
	private CacheProperties cacheProperties;

	
	@Bean
	public JedisProviderFactoryBean jedisProviderFactory(@Qualifier("jedisPoolConfig") JedisPoolConfig config) {

		JedisProviderFactoryBean bean = new JedisProviderFactoryBean();
		bean.setDatabase(cacheProperties.getDatabase());
		bean.setJedisPoolConfig(config);
		bean.setMode(cacheProperties.getMode());
		bean.setServers(cacheProperties.getServers());
		bean.setPassword(cacheProperties.getPassword());
		bean.setMasterName(cacheProperties.getMasterName());
		return bean;
	}

	@Bean(name = "jedisPoolConfig")
	public JedisPoolConfig jedisPoolConfig() {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(cacheProperties.getMaxPoolSize());
		config.setMaxIdle(cacheProperties.getMaxPoolIdle());
		config.setMinIdle(cacheProperties.getMinPoolIdle());
		config.setMaxWaitMillis(cacheProperties.getMaxPoolWaitMillis());
		return config;
	}
	
	
}
