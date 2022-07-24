/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.cache.adapter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.mendmix.cache.CacheAdapter;
import com.mendmix.cache.RedisTemplateGroups;
import com.mendmix.common.util.BeanUtils;


@SuppressWarnings({"unchecked" })
public class RedisCacheAdapter implements CacheAdapter {

	private RedisTemplate<String, Object> redisTemplate;
	private StringRedisTemplate stringRedisTemplate;
	
	public RedisCacheAdapter() {}

	public RedisCacheAdapter(RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate) {
		super();
		this.redisTemplate = redisTemplate;
		this.stringRedisTemplate = stringRedisTemplate;
	}
	
	public StringRedisTemplate getStringRedisTemplate() {
		if(stringRedisTemplate == null) {
			stringRedisTemplate = RedisTemplateGroups.getDefaultStringRedisTemplate();
		}
		return stringRedisTemplate;
	}
	

	public RedisTemplate<String, Object> getRedisTemplate() {
		if(redisTemplate == null) {
			redisTemplate = RedisTemplateGroups.getDefaultRedisTemplate();
		}
		return redisTemplate;
	}

	
	@Override
	public <T> T get(String key) {
		return (T) getRedisTemplate().opsForValue().get(key);
	}

	@Override
	public String getStr(String key) {
		return getStringRedisTemplate().opsForValue().get(key);
	}

	@Override
	public void set(String key, Object value, long expireSeconds) {
		getRedisTemplate().opsForValue().set(key, value, Duration.ofSeconds(expireSeconds));
	}

	@Override
	public void setStr(String key, String value, long expireSeconds) {
		getStringRedisTemplate().opsForValue().set(key, value, Duration.ofSeconds(expireSeconds));
	}

	@Override
	public void remove(String... keys) {
		if(keys == null || keys.length == 0 || keys[0] == null)return;
		if(keys.length == 1) {
			getRedisTemplate().delete(keys[0]);
		}else {
			getRedisTemplate().delete(Arrays.asList(keys));
		}
		
	}

	@Override
	public boolean exists(String key) {
		return getRedisTemplate().hasKey(key);
	}

	@Override
	public long size(String key) {
		return getRedisTemplate().opsForValue().size(key);
	}

	@Override
	public void setExpire(String key, long expireSeconds) {
		getRedisTemplate().expire(key, Duration.ofSeconds(expireSeconds));
	}

	@Override
	public boolean setIfAbsent(String key, Object value,long timeout,TimeUnit timeUnit) {
		if(BeanUtils.isSimpleDataType(value)) {
			return getStringRedisTemplate().opsForValue().setIfAbsent(key, value.toString(), timeout,timeUnit);
		}
		return getRedisTemplate().opsForValue().setIfAbsent(key, value, timeout,timeUnit);
	}


	@Override
	public void addStrItemToList(String key, String item) {
		getStringRedisTemplate().opsForList().leftPush(key, item);
	}


	@Override
	public List<String> getStrListItems(String key, int start, int end) {
		return getStringRedisTemplate().opsForList().range(key, start, end);
	}


	@Override
	public long getListSize(String key) {
		return getStringRedisTemplate().opsForList().size(key);
	}


	@Override
	public long getExpireIn(String key,TimeUnit timeUnit) {
		return getRedisTemplate().getExpire(key, timeUnit);
	}


	@Override
	public void setMapValue(String key, String field, Object value) {
		getRedisTemplate().opsForHash().put(key, field, value);
	}

	@Override
	public void setMapValues(String key,Map<String,Object> map) {
		getRedisTemplate().opsForHash().putAll(key, map);
	}

	@Override
	public <T> T getMapValue(String key, String field) {
		return (T) getRedisTemplate().opsForHash().get(key, field);
	}

	@Override
	public <T> Map<String, T> getMapValues(String key, Collection<String> fields) {
		if(fields == null || fields.isEmpty())return new HashMap<>(0);
		List<Object> hashKeys = new ArrayList<>(fields);
		Map<String, T> result = new HashMap<>(fields.size());
		List<Object> list = getRedisTemplate().opsForHash().multiGet(key, hashKeys);
        for (int i = 0; i < hashKeys.size(); i++) {
        	result.put(hashKeys.get(i).toString(), (T)list.get(i));
		}
        return result;
	}

	@Override
	public Set<String> getKeys(String pattern) {
		return getRedisTemplate().keys(pattern);
	}
	
}
