/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.cache.redis;

import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

import com.mendmix.cache.redis.cluster.JedisClusterProvider;
import com.mendmix.cache.redis.sentinel.JedisSentinelProvider;
import com.mendmix.cache.redis.standalone.JedisStandaloneProvider;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.spring.InstanceFactory;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * redis服务提供者注册工厂
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年04月26日
 */
public class JedisProviderFactoryBean implements ApplicationContextAware,InitializingBean{

	protected static final Logger logger = LoggerFactory.getLogger(JedisProviderFactoryBean.class);
	/**
	 * 
	 */
	public static final String DEFAULT_GROUP_NAME = "default";
	
	private static final String REDIS_PROVIDER_SUFFIX = "RedisProvider";
	
	private Pattern pattern = Pattern.compile("^.+[:]\\d{1,5}\\s*$");

	private String mode = JedisStandaloneProvider.MODE;//
	
	private JedisPoolConfig jedisPoolConfig;
	
	//用来区分不同组的缓存
	private String group;
	private String servers;
	private Integer timeout = 3000; //单位：毫秒
	private String password;
	private int database = Protocol.DEFAULT_DATABASE;
	private String masterName;
	private String clientName;
	private boolean tenantModeEnabled;
	
	private ApplicationContext context;

	public void setGroup(String group) {
		this.group = group;
	}

	public String getGroup() {
		if(group == null)group = DEFAULT_GROUP_NAME;
		return group;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void setJedisPoolConfig(JedisPoolConfig jedisPoolConfig) {
		this.jedisPoolConfig = jedisPoolConfig;
	}

	public void setServers(String servers) {
		this.servers = servers;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}
	
	
	public void setPassword(String password) {
		if(ResourceUtils.NULL_VALUE_PLACEHOLDER.equals(password))return;
		this.password = password;
	}

	public void setDatabase(int database) {
		this.database = database;
	}

	public void setMasterName(String masterName) {
		this.masterName = masterName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public void setTenantModeEnabled(boolean tenantModeEnabled) {
		this.tenantModeEnabled = tenantModeEnabled;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
		//
		InstanceFactory.setApplicationContext(context);;
	}
	
	
	

	@Override
	public void afterPropertiesSet() throws Exception {
		if(jedisPoolConfig == null)throw new Exception("jedisPoolConfig Not config ??");
		if(org.apache.commons.lang3.StringUtils.isAnyBlank(mode,servers)){
			throw new Exception("type or servers is empty??");
		}
		registerRedisProvier(); 
	}

	/**
	 * 
	 */
	private void registerRedisProvier() {
		String beanName = getGroup() + REDIS_PROVIDER_SUFFIX;
		if(context.containsBean(beanName)){
			throw new RuntimeException("已包含group为［"+this.group+"］的缓存实例");
		}
		
		String[] servers = StringUtils.tokenizeToStringArray(this.servers, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
		
		//检查ip和port格式
        for (String server : servers) {
			if(!pattern.matcher(server).matches()){
				throw new RuntimeException("参数servers："+this.servers+"错误");
			}
		}
        
		Class<?> beanClass = null;
		if(JedisStandaloneProvider.MODE.equalsIgnoreCase(mode)){	
			beanClass = JedisStandaloneProvider.class;
		}else if(JedisClusterProvider.MODE.equalsIgnoreCase(mode)){
			beanClass = JedisClusterProvider.class;
		}else if(JedisSentinelProvider.MODE.equalsIgnoreCase(mode)){
			beanClass = JedisSentinelProvider.class;
			//
			Validate.notBlank(masterName, "Sentinel模式[masterName]参数 required");
		}else{
			throw new RuntimeException("参数mode："+this.mode+"不支持");
		}
		
		
		DefaultListableBeanFactory acf = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();  
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
		beanDefinitionBuilder.addConstructorArgValue(getGroup())//
		        .addConstructorArgValue(jedisPoolConfig)//
		        .addConstructorArgValue(servers)//
		        .addConstructorArgValue(timeout);//
		
		if(JedisStandaloneProvider.MODE.equalsIgnoreCase(mode) 
				|| JedisSentinelProvider.MODE.equalsIgnoreCase(mode)){
			beanDefinitionBuilder.addConstructorArgValue(org.apache.commons.lang3.StringUtils.trimToNull(password))//
			    .addConstructorArgValue(database)//
			    .addConstructorArgValue(clientName);
		}
		
		if(JedisSentinelProvider.MODE.equalsIgnoreCase(mode)){
			beanDefinitionBuilder.addConstructorArgValue(masterName);
		}
		
		beanDefinitionBuilder.addPropertyValue("tenantModeEnabled", tenantModeEnabled);
		
		acf.registerBeanDefinition(beanName, beanDefinitionBuilder.getRawBeanDefinition());
		//
		logger.info("register JedisProvider OK,Class:{},beanName:{}",beanClass.getSimpleName(),beanName);
	}
}
