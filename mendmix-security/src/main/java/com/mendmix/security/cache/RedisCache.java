/*
 * Copyright 2016-2018 www.mendmix.com.
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
package com.mendmix.security.cache;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.mendmix.cache.RedisTemplateGroups;
import com.mendmix.security.Cache;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月8日
 */
public class RedisCache implements Cache {

	private static final String CACHE_GROUP_NAME = "mendmix.security";
	
	private StringRedisTemplate stringRedisTemplate;
	private RedisTemplate<String,Object> redisTemplate;
	private String keyPrefix;
	private Duration timeToLiveSeconds;
	
	
	public RedisCache(String keyPrefix,int timeToLiveSeconds) {
		this.stringRedisTemplate = RedisTemplateGroups.getStringRedisTemplate(CACHE_GROUP_NAME);
		this.redisTemplate = RedisTemplateGroups.getRedisTemplate(CACHE_GROUP_NAME);
		this.keyPrefix = keyPrefix + ":";
		this.timeToLiveSeconds = Duration.ofSeconds(timeToLiveSeconds);
	}
	
	private String buildKey(String key){
		return this.keyPrefix.concat(key);
	}

	@Override
	public void setString(String key, String value) {
		stringRedisTemplate.opsForValue().set(buildKey(key), value, timeToLiveSeconds);
	}

	@Override
	public String getString(String key) {
		return stringRedisTemplate.opsForValue().get(buildKey(key));
	}

	@Override
	public void setObject(String key, Object value) {
		redisTemplate.opsForValue().set(buildKey(key), value, timeToLiveSeconds);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getObject(String key) {
		return (T) redisTemplate.opsForValue().get(buildKey(key));
	}

	@Override
	public void remove(String key) {
		redisTemplate.delete(buildKey(key));
	}

	@Override
	public void removeAll() {
		
	}

	@Override
	public boolean exists(String key) {
		return redisTemplate.hasKey(buildKey(key));
	}

	@Override
	public void setMapValue(String key,String field,Object value) {
		key = buildKey(key);
		redisTemplate.opsForHash().put(key, field, value);
		redisTemplate.expire(key, timeToLiveSeconds);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getMapValue(String key, String field) {
		return (T) redisTemplate.opsForHash().get(buildKey(key), field);
	}


	@Override
	public void updateExpireTime(String key) {
		redisTemplate.expire(buildKey(key), timeToLiveSeconds);
	}

}
