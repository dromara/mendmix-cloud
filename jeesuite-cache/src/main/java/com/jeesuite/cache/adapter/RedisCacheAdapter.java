/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.jeesuite.cache.adapter;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.jeesuite.cache.CacheAdapter;

@SuppressWarnings({ "unchecked" })
public class RedisCacheAdapter implements CacheAdapter{

	private RedisTemplate<String, Object> redisTemplate;
	private StringRedisTemplate stringRedisTemplate;
	
	
	
	public RedisCacheAdapter() {}

	public RedisCacheAdapter(RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate) {
		super();
		this.redisTemplate = redisTemplate;
		this.stringRedisTemplate = stringRedisTemplate;
	}
	
	public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public <T> T get(String key) {
		return (T) redisTemplate.opsForValue().get(key);
	}

	@Override
	public String getString(String key) {
		return stringRedisTemplate.opsForValue().get(key);
	}

	@Override
	public void set(String key, Object value, long expireSeconds) {
		redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
	}

	@Override
	public void setString(String key, Object value, long expireSeconds) {
		if(value == null)return;
		stringRedisTemplate.opsForValue().set(key, value.toString(), expireSeconds, TimeUnit.SECONDS);
	}

	@Override
	public void remove(String... keys) {
		if(keys.length == 1) {
			redisTemplate.delete(keys[0]);
		}
		redisTemplate.delete(Arrays.asList(keys));
	}

	@Override
	public boolean exists(String key) {
		return redisTemplate.hasKey(key);
	}

	@Override
	public void addListItems(String key, String... items) {
		stringRedisTemplate.opsForList().leftPushAll(key, items);
	}

	@Override
	public List<String> getListItems(String key, int start, int end) {
		return stringRedisTemplate.opsForList().range(key, start, end);
	}

	@Override
	public long getListSize(String key) {
		return stringRedisTemplate.opsForList().size(key);
	}

	@Override
	public boolean setIfAbsent(String key, String value, long expireSeconds) {
		return stringRedisTemplate.opsForValue().setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS);
	}
	
	@Override
	public Map<String, String> getMap(String key) {
		Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
		Set<Object> fields = entries.keySet();
		Map<String, String> result = new HashMap<>(fields.size());
		for (Object field : fields) {
			result.put(field.toString(), entries.get(field).toString());
		}
		return result;
	}

	@Override
	public void setMapItem(String key, String field, String value) {
		stringRedisTemplate.opsForHash().put(key, field, value);
	}

	@Override
	public String getMapItem(String key, String field) {
		Object value = stringRedisTemplate.opsForHash().get(key, field);
		return value == null ? null : value.toString();
	}

	@Override
	public void setExpire(String key, long expireSeconds) {
		redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
	}

	@Override
	public void setExpireAt(String key, Date expireAt) {
		redisTemplate.expireAt(key, expireAt);
	}

	@Override
	public long getTtl(String key) {
		return redisTemplate.getExpire(key, TimeUnit.SECONDS);
	}

}
