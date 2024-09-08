/*
 * Copyright 2016-2022 dromara.org.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.dromara.mendmix.cache.CacheAdapter;

@SuppressWarnings("unchecked")
public class LocalCacheAdapter implements CacheAdapter {
	
	private int timeToLiveSeconds;
	
	private static LocalCacheAdapter _1HourCache;
	private static LocalCacheAdapter _1MinCache;
	private static LocalCacheAdapter _10SecondCache;
	
	public static LocalCacheAdapter secondCache() {
		if(_10SecondCache != null)return _10SecondCache;
		synchronized (LocalCacheAdapter.class) {
			_10SecondCache = new LocalCacheAdapter(10);
		}
		return _10SecondCache;
	}

	public static LocalCacheAdapter miniteCache() {
		if(_1MinCache != null)return _1MinCache;
		synchronized (LocalCacheAdapter.class) {
			 _1MinCache = new LocalCacheAdapter(60);
		}
		return _1MinCache;
	}
	
	public static LocalCacheAdapter hourCache() {
		if(_1HourCache != null)return _1HourCache;
		synchronized (LocalCacheAdapter.class) {
			_1HourCache = new LocalCacheAdapter(3600);
		}
		return _1HourCache;
	}

	private Cache<String,Object> cache;
	
	
	public Cache<String, Object> getCache() {
		return cache;
	}

	public LocalCacheAdapter(int timeToLiveSeconds) {
		this(timeToLiveSeconds, 10000);
	}
	
	public LocalCacheAdapter(int timeToLiveSeconds,long maximumSize) {
		this.timeToLiveSeconds = timeToLiveSeconds;
		cache =  CacheBuilder
				.newBuilder()
				.maximumSize(maximumSize)
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
	
	public <T> T get(String key,Callable<T> loader) {
		try {
			T value = get(key);
			if(value == null) {
				value = loader.call();
				if(value != null) {
					set(key, value, 0);
				}
			}
			return value;
			//return (T) cache.get(key, loader);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getStr(String key) {
		return get(key);
	}

	@Override
	public void set(String key, Object value, long expireSeconds) {
		if(expireSeconds <= 0 || expireSeconds > timeToLiveSeconds) {
			expireSeconds = timeToLiveSeconds;
		}
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
		List<String> items = getExpireableCacheStringList(key);
		if(items == null) {
			items = new ArrayList<>();
			ExpireableObject expireableObject = new ExpireableObject(items, timeToLiveSeconds);
			cache.put(key, expireableObject);
		}
		items.add(item);
	}

	@Override
	public List<String> getStrListItems(String key, int start, int end) {
		List<String> items = getExpireableCacheStringList(key);
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
		List<String> items = getExpireableCacheStringList(key);
		return items == null ? 0 : items.size();
	}
	
	@Override
	public void removeListValue(String key,String value) {
		List<String> items = getExpireableCacheStringList(key);
		if(items == null || items.isEmpty())return;
		Iterator<String> iterator = items.iterator();
		while(iterator.hasNext()) {
			if(iterator.next().equals(value)) {
				iterator.remove();
			}
		}
	}
	
	@Override
	public long getExpireIn(String key,TimeUnit timeUnit) {
		Object value = cache.getIfPresent(key);
		if(value == null)return -1L;
		if(value instanceof ExpireableObject == false) {
			return -1L;
		}
		ExpireableObject expireableObject = (ExpireableObject) value;
		long millis = expireableObject.getExpireAt() - System.currentTimeMillis();
		if(millis < 0)return -1L;
		return timeUnit.convert(millis, TimeUnit.MILLISECONDS);
	}

	@Override
	public void setMapValue(String key, String field, Object value) {
		Map<String, Object> map = getExpireableCacheMap(key);	
		if(map == null) {
			map = new HashMap<String, Object>();
			ExpireableObject expireableObject = new ExpireableObject(map, timeToLiveSeconds);
			cache.put(key, expireableObject);
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
		Map<String, Object> entries = getExpireableCacheMap(key);
		return entries == null ? null : (T)entries.get(field);
	}

	@Override
	public <T> Map<String, T> getMapValues(String key) {
		Map<String, Object> entries = getExpireableCacheMap(key);
		if(entries == null)return new HashMap<>(0);
		Map<String, T> result = new HashMap<>(entries.size());
		entries.forEach( (k,v) -> {
			result.put(k.toString(), (T)v);
		} );
		return result;
	}
	
	@Override
	public <T> Map<String, T> getMapValues(String key, Collection<String> fields) {
		if(fields == null || fields.isEmpty())return new HashMap<>(0);
		Map<String, T> result = new HashMap<>(fields.size());
        for (String field : fields) {
        	result.put(field, getMapValue(key, field));
		}
        return result;
	}
	
	@Override
	public void removeMapValue(String key, String field) {
		Map<String, Object> entries = getExpireableCacheMap(key);
		if(entries != null)entries.remove(field);
	}
	
	@Override
	public boolean hasMapValue(String key, String field) {
		Map<String, Object> entries = getExpireableCacheMap(key);
		return entries != null && entries.containsKey(field);
	}

	@Override
	public Set<String> getKeys(String pattern) {
		return new HashSet<>(0);
	}

	@Override
	public void setMapStringValue(String key, String field, String value) {
		setMapValue(key, field, value);
	}

	@Override
	public void setMapStringValues(String key, Map<String, String> map) {
		map.forEach( (field, value) -> {
			setMapValue(key, field, value);
		} );
	}

	@Override
	public String getMapStringValue(String key, String field) {
		return getMapValue(key, field);
	}
	
	@Override
	public Map<String, String> getMapStringValues(String key) {
		return getMapValues(key);
	}

	@Override
	public Map<String, String> getMapStringValues(String key, Collection<String> fields) {
		return getMapValues(key, fields);
	}

	private Map<String, Object> getExpireableCacheMap(String key) {
		Object value = cache.getIfPresent(key);
		if(value == null)return null;
		Map<String, Object> entries;
		if(value instanceof ExpireableObject) {
			ExpireableObject expireableObject = (ExpireableObject) value;
			if(expireableObject.getExpireAt() < System.currentTimeMillis()) {
				cache.invalidate(key);
				return null;
			}
			entries = (Map<String, Object>) expireableObject.getTarget();
		}else {
			entries = (Map<String, Object>) cache.getIfPresent(key);
		}
		return entries;
	}
	
	private List<String> getExpireableCacheStringList(String key) {
		Object value = cache.getIfPresent(key);
		if(value == null)return null;
		List<String> entries;
		if(value instanceof ExpireableObject) {
			ExpireableObject expireableObject = (ExpireableObject) value;
			if(expireableObject.getExpireAt() < System.currentTimeMillis()) {
				cache.invalidate(key);
				return null;
			}
			entries = (List<String>) expireableObject.getTarget();
		}else {
			entries = (List<String>) cache.getIfPresent(key);
		}
		return entries;
	}
}
