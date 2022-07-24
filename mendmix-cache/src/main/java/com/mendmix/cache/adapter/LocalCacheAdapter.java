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
package com.mendmix.cache.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mendmix.cache.CacheAdapter;

@SuppressWarnings("unchecked")
public class LocalCacheAdapter implements CacheAdapter {

	private Cache<String,Object> cache;
	
	
	public Cache<String, Object> getCache() {
		return cache;
	}

	public LocalCacheAdapter(int timeToLiveSeconds) {
		cache =  CacheBuilder
				.newBuilder()
				.maximumSize(10000)
				.expireAfterWrite(timeToLiveSeconds, TimeUnit.SECONDS)
				.build();
	}
	
	@Override
	public <T> T get(String key) {
		Object value = cache.getIfPresent(key);
		if(value == null)return null;
		try {
			ExpireableObject expireableObject = (ExpireableObject) value;
			if(expireableObject.getExpireAt() < System.currentTimeMillis()) {
				cache.invalidate(key);
				return null;
			}
			return (T) expireableObject.getTarget();
		} catch (ClassCastException e) {
			return (T) value;
		}
		
	}

	@Override
	public String getStr(String key) {
		return get(key);
	}

	@Override
	public void set(String key, Object value, long expireSeconds) {
		ExpireableObject expireableObject = new ExpireableObject(value, expireSeconds);
		cache.put(key, expireableObject);
	}

	@Override
	public void setStr(String key, String value, long expireSeconds) {
		set(key, value, expireSeconds);
	}

	@Override
	public void remove(String... keys) {
		if(keys != null && keys.length > 0 && keys[0] != null) {
			cache.invalidateAll(Arrays.asList(keys));
		}else {
			cache.invalidateAll();
		}
	}

	@Override
	public boolean exists(String key) {
		return get(key) != null;
	}

	@Override
	public void setExpire(String key, long expireSeconds) {
		Object value = cache.getIfPresent(key);
		if(value == null)return;
		if(value instanceof ExpireableObject) {
			((ExpireableObject)value).setExpireAt(System.currentTimeMillis() + expireSeconds * 1000);
		}
		
	}

	@Override
	public boolean setIfAbsent(String key, Object value,long timeout,TimeUnit timeUnit) {
		if(exists(key))return false;
		set(key, value, timeUnit.toSeconds(timeout));
		return true;
	}


	@Override
	public long size(String key) {
		return cache.size();
	}

	@Override
	public void addStrItemToList(String key, String item) {
		List<String> items = (List<String>) cache.getIfPresent(key);
		if(items == null) {
			items = new ArrayList<>();
			cache.put(key, items);
		}
		items.add(item);
	}

	@Override
	public List<String> getStrListItems(String key, int start, int end) {
		List<String> items = (List<String>) cache.getIfPresent(key);
		if(items == null)return new ArrayList<>(0);
		if(start >= items.size()) {
			return items;
		}
		if(end >= items.size()) {
			end = items.size() - 1;
		}
		return items.subList(start, end);
	}

	@Override
	public long getListSize(String key) {
		List<String> items = (List<String>) cache.getIfPresent(key);
		return items == null ? 0 : items.size();
	}
	
	@Override
	public long getExpireIn(String key,TimeUnit timeUnit) {
		ExpireableObject expireableObject = (ExpireableObject) cache.getIfPresent(key);
		if(expireableObject == null)return -1L;
		long millis = expireableObject.getExpireAt() - System.currentTimeMillis();
		if(millis < 0)return -1L;
		return timeUnit.convert(millis, TimeUnit.MILLISECONDS);
	}

	@Override
	public void setMapValue(String key, String field, Object value) {
		Map<String, Object> map = (Map<String, Object>) cache.getIfPresent(key);	
		if(map == null) {
			map = new HashMap<String, Object>();
			cache.put(key, map);
		}
		map.put(field, value);
	}

	@Override
	public void setMapValues(String key, Map<String,Object> map) {
		map.forEach( (field, value) -> {
			setMapValue(key, field, value);
		} );
	}
	
	@Override
	public <T> T getMapValue(String key, String field) {
		Map<String, Object> map = (Map<String, Object>) cache.getIfPresent(key);
		return map == null ? null : (T)map.get(field);
	}
	
	@Override
	public <T> Map<String, T> getMapValues(String key, Collection<String> fields) {
		if(fields == null || fields.isEmpty())return new HashMap<>(0);
		Map<String, T> result = new HashMap<>(fields.size());
		T value;
        for (String field : fields) {
        	value = getMapValue(key, field);
        	if(value == null)continue;
        	result.put(field, value);
		}
        return result;
	}

	@Override
	public Set<String> getKeys(String pattern) {
		return new HashSet<>(0);
	}
	
	
}
