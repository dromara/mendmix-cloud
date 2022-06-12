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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.mendmix.cache.redis.cluster.JedisClusterProvider;
import com.mendmix.cache.redis.sentinel.JedisSentinelProvider;
import com.mendmix.cache.redis.standalone.JedisStandaloneProvider;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.spring.InstanceFactory;

import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.commands.BinaryJedisClusterCommands;
import redis.clients.jedis.commands.BinaryJedisCommands;
import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.commands.MultiKeyBinaryCommands;
import redis.clients.jedis.commands.MultiKeyBinaryJedisClusterCommands;
import redis.clients.jedis.commands.MultiKeyCommands;
import redis.clients.jedis.commands.MultiKeyJedisClusterCommands;

/**
 * redis实例工厂
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年04月26日
 */
public class JedisProviderFactory {

	protected static final Logger logger = LoggerFactory.getLogger(JedisProviderFactory.class);

	private static JedisProvider<?, ?> defaultJedisProvider;
	
	private static  boolean inited = false;
	
	@SuppressWarnings("rawtypes")
	private static Map<String, JedisProvider> jedisProviders = new ConcurrentHashMap<>();
	
	public static void setDefaultJedisProvider(JedisProvider<?, ?> defaultJedisProvider) {
		JedisProviderFactory.defaultJedisProvider = defaultJedisProvider;
	}
	
	public static boolean containsGroup(String groupName){
		if(jedisProviders.isEmpty()){
			try {initFactoryFromSpring();} catch (Exception e) {}
		}
		return jedisProviders.containsKey(groupName);
	}
	
	public synchronized static void addProvider(JedisProvider<?, ?> provider){
		Assert.isTrue(!jedisProviders.containsKey(provider.groupName()), String.format("group[%s]已存在", provider.groupName()));
		jedisProviders.put(provider.groupName(), provider);
	}

	public static JedisProvider<?, ?> getJedisProvider(String groupName) {
		
		if(StringUtils.isNotBlank(groupName) && jedisProviders.containsKey(groupName)){
			return jedisProviders.get(groupName);
		}
		
		if(defaultJedisProvider == null){			
			initFactoryFromSpring();
		}
		
		if(StringUtils.isNotBlank(groupName)){
			if(jedisProviders.containsKey(groupName)){
				return jedisProviders.get(groupName);
			}
			logger.warn("MENDMIX-TRACE-LOGGGING-->> 未找到group[{}]对应的redis配置，使用默认缓存配置",groupName);
		}
		return defaultJedisProvider;
	}

	@SuppressWarnings("rawtypes")
	private synchronized static void initFactoryFromSpring() {
		if(inited)return;
		if(defaultJedisProvider == null){
			Map<String, JedisProvider> interfaces = InstanceFactory.getBeansOfType(JedisProvider.class);
			
			if(interfaces != null && interfaces.size() >0){				
				Iterator<JedisProvider> iterator = interfaces.values().iterator();
				while(iterator.hasNext()){
					JedisProvider jp = iterator.next();
					jedisProviders.put(jp.groupName(), jp);
				}
			}
			defaultJedisProvider = jedisProviders.get(JedisProviderFactoryBean.DEFAULT_GROUP_NAME);
			if(defaultJedisProvider == null && jedisProviders.size() == 1){
				defaultJedisProvider = new ArrayList<>(jedisProviders.values()).get(0);
			}
			
			Assert.notNull(defaultJedisProvider,"无默认缓存配置，请指定一组缓存配置group为default");
		}
		
		inited = true;
	}

	public static JedisCommands getJedisCommands(String groupName) {
		return (JedisCommands) getJedisProvider(groupName).get();
	}

	public static BinaryJedisCommands getBinaryJedisCommands(String groupName) {
		return (BinaryJedisCommands) getJedisProvider(groupName).getBinary();
	}

	public static BinaryJedisClusterCommands getBinaryJedisClusterCommands(String groupName) {
		return (BinaryJedisClusterCommands) getJedisProvider(groupName).getBinary();
	}

	public static JedisCommands getJedisClusterCommands(String groupName) {
		return (JedisCommands) getJedisProvider(groupName).get();
	}
	
	public static MultiKeyCommands getMultiKeyCommands(String groupName) {
		return (MultiKeyCommands) getJedisProvider(groupName).get();
	}
	
	public static MultiKeyBinaryCommands getMultiKeyBinaryCommands(String groupName) {
		return (MultiKeyBinaryCommands) getJedisProvider(groupName).get();
	}
	
	public static MultiKeyJedisClusterCommands getMultiKeyJedisClusterCommands(String groupName) {
		return (MultiKeyJedisClusterCommands) getJedisProvider(groupName).get();
	}
	
	public static MultiKeyBinaryJedisClusterCommands getMultiKeyBinaryJedisClusterCommands(String groupName) {
		return (MultiKeyBinaryJedisClusterCommands) getJedisProvider(groupName).get();
	}
	
	public static String currentMode(String groupName){
		return getJedisProvider(groupName).mode();
	}
	
	public static boolean isStandalone(String groupName){
		return JedisStandaloneProvider.MODE.equals(currentMode(groupName));
	}
	
	public static boolean isCluster(String groupName){
		return JedisClusterProvider.MODE.equals(currentMode(groupName));
	}
	
	public static synchronized void addGroupProvider(String groupName){
		if(containsGroup(groupName))return;
		String prefix =  groupName + ".cache." ;
		String mode = ResourceUtils.getProperty(prefix + "mode","standalone");
		String server = ResourceUtils.getAndValidateProperty(prefix + "servers");
		String datebase = ResourceUtils.getAndValidateProperty(prefix + "database");
		String password = ResourceUtils.getProperty(prefix + "password");
		String maxPoolSize = ResourceUtils.getProperty(prefix + "maxPoolSize","50");
		
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(1);
		poolConfig.setMinEvictableIdleTimeMillis(30*60*1000);
		poolConfig.setMaxTotal(Integer.parseInt(maxPoolSize));
		poolConfig.setMaxWaitMillis(5 * 1000);
		
		String[] servers = server.split(";|,");
		int timeout = 3000;
		if("standalone".equals(mode)){
			JedisProvider<Jedis,BinaryJedis> provider = new JedisStandaloneProvider(groupName, poolConfig, servers, timeout, password, Integer.parseInt(datebase),null);
			JedisProviderFactory.addProvider(provider);
		}else if("sentinel".equals(mode)){
			String masterName = ResourceUtils.getProperty(prefix + "masterName");
			Validate.notBlank(masterName, "[masterName] not found");
			JedisSentinelProvider provider = new JedisSentinelProvider(groupName, poolConfig, servers, timeout, password, Integer.parseInt(datebase), null, masterName);
			JedisProviderFactory.addProvider(provider);
		}
	}
}
