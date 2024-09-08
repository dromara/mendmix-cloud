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
package org.dromara.mendmix.cache.adapter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.dromara.mendmix.cache.CacheAdapter;
import org.dromara.mendmix.cache.RedisTemplateGroups;
import org.dromara.mendmix.common.util.BeanUtils;


@SuppressWarnings({"unchecked" })
public class RedisCacheAdapter implements CacheAdapter {
	
	private static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix");

	private RedisTemplate<String, Object> redisTemplate;
	private StringRedisTemplate stringRedisTemplate;
	
	public RedisCacheAdapter() {}

	public RedisCacheAdapter(RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate) {
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
		try {
			return (T) getRedisTemplate().opsForValue().get(key);
		} catch (org.springframework.data.redis.serializer.SerializationException e) {
			getRedisTemplate().delete(key);
			logger.warn(">> redis key:{} exists,but value can't deserialize",key);
			return null;
		}
	}

	@Override
	public String getStr(String key) {
		return getStringRedisTemplate().opsForValue().get(key);
	}

	@Override
	public void set(String key, Object value, long expireSeconds) {
		try {
			if(expireSeconds > 0) {
				getRedisTemplate().opsForValue().set(key, value, Duration.ofSeconds(expireSeconds));
			}else {
				getRedisTemplate().opsForValue().set(key, value);
			}
		} catch (Exception e) {
			logger.warn("set cache for:[{}] error -> {}",key,e.getMessage());
		}
	}

	@Override
	public void setStr(String key, String value, long expireSeconds) {
		if(expireSeconds > 0) {
			getStringRedisTemplate().opsForValue().set(key, value, Duration.ofSeconds(expireSeconds));
		}else {
			getStringRedisTemplate().opsForValue().set(key, value);
		}
		
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
	public void removeListValue(String key,String value) {
		getStringRedisTemplate().opsForList().remove(key, 0, value);
	}

	@Override
	public long getExpireIn(String key,TimeUnit timeUnit) {
		Long expire = getRedisTemplate().getExpire(key, timeUnit);
		return expire == null ? 0 : expire;
	}


	@Override
	public void setMapValue(String key, String field, Object value) {
		getRedisTemplate().opsForHash().put(key, field, value);
	}

	@Override
	public void setMapValues(String key,Map<String,Object> map) {
		if(map == null || map.isEmpty())return;
		getRedisTemplate().opsForHash().putAll(key, map);
	}

	@Override
	public <T> T getMapValue(String key, String field) {
		try {
			return (T) getRedisTemplate().opsForHash().get(key, field);
		} catch (Exception e) {
			logger.warn(">> redis key:{} exists,but value can't deserialize",key);
			return null;
		}
	}
	
	@Override
	public <T> Map<String, T> getMapValues(String key) {
		Map<Object, Object> entries = getRedisTemplate().opsForHash().entries(key);
		Map<String, T> result = new HashMap<>(entries.size());
		entries.forEach( (k,v) -> {
			result.put(k.toString(), (T)v);
		} );
		return result;
	}

	@Override
	public <T> Map<String, T> getMapValues(String key, Collection<String> fields) {
		if(fields == null || fields.isEmpty())return new HashMap<>(0);
		List<Object> hashKeys = new ArrayList<>(fields);
		Map<String, T> result = new HashMap<>(fields.size());
		try {
			List<Object> list = getRedisTemplate().opsForHash().multiGet(key, hashKeys);
	        for (int i = 0; i < hashKeys.size(); i++) {
	        	result.put(hashKeys.get(i).toString(), (T)list.get(i));
			}
		} catch (Exception e) {
			logger.warn(">> redis key:{} exists,but value can't deserialize",key);
		}
        return result;
	}
	
	@Override
	public void removeMapValue(String key, String field) {
		getRedisTemplate().opsForHash().delete(key, field);
	}
	
	@Override
	public boolean hasMapValue(String key, String field) {
		return getRedisTemplate().opsForHash().hasKey(key, field);
	}

	@Override
	public Set<String> getKeys(String pattern) {
		return getRedisTemplate().keys(pattern);
	}
	
	@Override
	public void setMapStringValue(String key, String field, String value) {
		getStringRedisTemplate().opsForHash().put(key, field, value);
	}

	@Override
	public void setMapStringValues(String key, Map<String, String> map) {
		if(map == null || map.isEmpty())return;
		getStringRedisTemplate().opsForHash().putAll(key, map);
	}

	@Override
	public String getMapStringValue(String key, String field) {
		return (String) getStringRedisTemplate().opsForHash().get(key, field);
	}
	
	@Override
	public Map<String, String> getMapStringValues(String key) {
		Map<Object, Object> entries = getStringRedisTemplate().opsForHash().entries(key);
		Map<String, String> result = new HashMap<>(entries.size());
		entries.forEach( (k,v) -> {
			result.put(k.toString(), Objects.toString(v, null));
		} );
		return result;
	}

	@Override
	public Map<String, String> getMapStringValues(String key, Collection<String> fields) {
		if(fields == null || fields.isEmpty())return new HashMap<>(0);
		List<Object> hashKeys = new ArrayList<>(fields.size());
        for (String field : fields) {
        	hashKeys.add(field);
		}
        Map<String, String> result = new HashMap<>(fields.size());
		try {
			List<Object> list = getStringRedisTemplate().opsForHash().multiGet(key, hashKeys);
	        for (int i = 0; i < hashKeys.size(); i++) {
	        	result.put(hashKeys.get(i).toString(), Objects.toString(list.get(i), null));
			}
		} catch (Exception e) {
			logger.warn(">> redis key:{} exists,but value can't deserialize",key);
		}
        return result;
	}

	
}
