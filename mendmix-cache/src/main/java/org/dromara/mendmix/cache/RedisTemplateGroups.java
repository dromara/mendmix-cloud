/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.spring.InstanceFactory;

import redis.clients.jedis.JedisPoolConfig;

public class RedisTemplateGroups {

	private static final String DEFAULT_CACHE_NAME = "default";
	
	private static Map<String, TemplatePair> groupTemplateMapping = new HashMap<>();
	
	
	public static List<String> getRedisTemplateGroupNames() {
		return new ArrayList<>(groupTemplateMapping.keySet());
	}
	
	public static StringRedisTemplate getDefaultStringRedisTemplate() {
		return getStringRedisTemplate(DEFAULT_CACHE_NAME);
	}
	
	public static RedisTemplate<String, Object> getDefaultRedisTemplate() {
		return getRedisTemplate(DEFAULT_CACHE_NAME);
	}
	
	public static StringRedisTemplate getStringRedisTemplate(String groupName) {
		TemplatePair pair = groupTemplateMapping.get(groupName);
		if(pair != null) {
			return pair.stringRedisTemplate;
		}
		
		if(groupTemplateMapping.containsKey(groupName)) {
			throw new MendmixBaseException("redisedisTemplate For["+groupName+"] not found");
		}else {
			initRedisTemplateGroup(groupName);
		}
		
		return getStringRedisTemplate(groupName);
	}
	

	public static RedisTemplate<String, Object> getRedisTemplate(String groupName) {
		TemplatePair pair = groupTemplateMapping.get(groupName);
		if(pair != null) {
			return pair.redisTemplate;
		}
		
		if(groupTemplateMapping.containsKey(groupName)) {
			throw new MendmixBaseException("redisedisTemplate For["+groupName+"] not found");
		}else {
			initRedisTemplateGroup(groupName);
		}
		
		return getRedisTemplate(groupName);
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static synchronized void initRedisTemplateGroup(String groupName) {
		
		if(groupTemplateMapping.containsKey(groupName))return;
		
		RedisTemplate<String, Object> redisTemplate = null;
		StringRedisTemplate stringRedisTemplate = null;
		if(DEFAULT_CACHE_NAME.equals(groupName) && CacheUtils.isRedis()) {
			Map<String, RedisOperations> instanceMap = InstanceFactory.getBeansOfType(RedisOperations.class);
			Collection<RedisOperations> instances = instanceMap.values();
			for (RedisOperations redis : instances) {
				if(redis instanceof StringRedisTemplate) {
					stringRedisTemplate = (StringRedisTemplate) redis;
				}else {
					redisTemplate = (RedisTemplate<String, Object>) redis;
				}
			}
		
		}else {
			if(!ResourceUtils.containsAnyProperty(
					 groupName + ".redis.host"
					,groupName + ".redis.sentinel.nodes"
					,groupName + ".redis.cluster.nodes")
			) {
				groupTemplateMapping.put(groupName, null);
				return;
			}
			
			JedisPoolConfig poolConfig = new JedisPoolConfig();
			poolConfig.setMaxTotal(ResourceUtils.getInt(groupName + ".redis.jedis.pool.max-active", 10));
			poolConfig.setMinIdle(ResourceUtils.getInt(groupName + ".redis.jedis.pool.min-idle", 1));
			poolConfig.setMaxWaitMillis(ResourceUtils.getLong(groupName + ".redis.jedis.pool.max-wait", 30000));
			int timeout = ResourceUtils.getInt(groupName + ".redis.timeout", 2000);
			
			JedisClientConfiguration clientConfiguration = JedisClientConfiguration.builder()
					.connectTimeout(Duration.ofMillis(timeout))
					.readTimeout(Duration.ofMillis(timeout))
					.usePooling().poolConfig(poolConfig)
					.build();
			
			int database = ResourceUtils.getInt(groupName + ".redis.database", 0);
			String password = StringUtils.trimToNull(ResourceUtils.getProperty(groupName + ".redis.password"));			
			JedisConnectionFactory redisConnectionFactory;
			if(ResourceUtils.containsProperty(groupName + ".redis.host")) {
				String host = ResourceUtils.getProperty(groupName + ".redis.host");
				int port = ResourceUtils.getInt(groupName + ".redis.port", 6379);
				
				RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);
				standaloneConfig.setDatabase(database);
				standaloneConfig.setPassword(password);
				redisConnectionFactory = new JedisConnectionFactory(standaloneConfig,clientConfiguration);	
			}else if(ResourceUtils.containsProperty(groupName + ".redis.sentinel.nodes")) {
				String master = ResourceUtils.getAndValidateProperty(groupName + ".redis.sentinel.master");
				Set<String> hostAndPorts = new HashSet<>(ResourceUtils.getList(groupName + ".redis.sentinel.nodes"));
				RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration(master, hostAndPorts);
				sentinelConfig.setDatabase(database);
				sentinelConfig.setPassword(password);
				redisConnectionFactory = new JedisConnectionFactory(sentinelConfig, clientConfiguration);
			}else if(ResourceUtils.containsProperty(groupName + ".redis.cluster.nodes")) {
				List<String> clusterNodes = ResourceUtils.getList(groupName + ".redis.cluster.nodes");
				RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(clusterNodes);
				clusterConfig.setPassword(password);
				redisConnectionFactory = new JedisConnectionFactory(clusterConfig, clientConfiguration);
			}else {
				throw new RuntimeException("无redis分组["+groupName+"]服务器相关配置");
			}
			
			redisConnectionFactory.afterPropertiesSet();
			
			redisTemplate = new RedisTemplate<>();
			redisTemplate.setConnectionFactory(redisConnectionFactory);
			redisTemplate.setKeySerializer(new StringRedisSerializer());
			redisTemplate.setHashKeySerializer(new StringRedisSerializer());
			
			redisTemplate.afterPropertiesSet();
			
			stringRedisTemplate = new StringRedisTemplate();
			stringRedisTemplate.setKeySerializer(new StringRedisSerializer());
			stringRedisTemplate.setHashKeySerializer(new StringRedisSerializer());
			stringRedisTemplate.setHashValueSerializer(new StringRedisSerializer());
			stringRedisTemplate.setConnectionFactory(redisConnectionFactory);
			
			stringRedisTemplate.afterPropertiesSet();
		}
		
		if(redisTemplate == null)return;
		
	    TemplatePair pair = new TemplatePair();
	    pair.redisTemplate = redisTemplate;
	    pair.stringRedisTemplate = stringRedisTemplate;
	    
	    groupTemplateMapping.put(groupName, pair);
	    
	}
	
	private static class TemplatePair{
		RedisTemplate<String, Object> redisTemplate;
		StringRedisTemplate stringRedisTemplate;
	}
}
