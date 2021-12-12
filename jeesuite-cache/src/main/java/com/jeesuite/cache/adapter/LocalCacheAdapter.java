package com.jeesuite.cache.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jeesuite.cache.CacheAdapter;
import com.jeesuite.cache.CacheExpires;

@SuppressWarnings("unchecked")
public class LocalCacheAdapter implements CacheAdapter {

	private Cache<String, Object> cache;

	public LocalCacheAdapter() {
		cache = CacheBuilder.newBuilder()  //
				.maximumSize(10000)  //
				.expireAfterWrite(CacheExpires.IN_1DAY, TimeUnit.SECONDS)  //
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
	public String getString(String key) {
		return get(key);
	}

	@Override
	public void set(String key, Object value, long expireSeconds) {
		ExpireableObject expireableObject = new ExpireableObject(value, expireSeconds);
		cache.put(key, expireableObject);
	}

	@Override
	public void setString(String key, Object value, long expireSeconds) {
		set(key, value, expireSeconds);
	}

	@Override
	public void remove(String... keys) {
		if(keys.length == 1) {
			cache.invalidate(keys[0]);
		}else {
			cache.invalidateAll(Arrays.asList(keys));	
		}
		
	}

	@Override
	public boolean exists(String key) {
		return get(key) != null;
	}

	@Override
	public void addListItems(String key, String... items) {
		List<String> list = get(key);
		if(list == null) {
			synchronized (cache) {
				list = new ArrayList<>();
				cache.put(key, list);
			}
		}
		
		for (String item : items) {
			list.add(item);
		}
	}

	@Override
	public List<String> getListItems(String key, int start, int end) {
		List<String> list = get(key);
		if(list == null)return new ArrayList<>(0);
		if(list.size() <= end + 1)return list;
		return list.subList(start, end);
	}

	@Override
	public long getListSize(String key) {
		List<String> list = get(key);
		return list == null ? 0 : list.size();
	}

	@Override
	public boolean setIfAbsent(String key, String value, long expireSeconds) {
		if(exists(key))return false;
		set(key, value, expireSeconds);
		return true;
	}

	@Override
	public void setMapItem(String key, String field, String value)  {
		Map<String, String> map = getMap(key);
		if(map == null) {
			synchronized (cache) {
				map = new HashMap<>();
				cache.put(key, map);
			}
		}
		map.put(field, value);
	}

	@Override
	public Map<String, String> getMap(String key) {
		return get(key);
	}

	@Override
	public String getMapItem(String key, String field) {
		Map<String, String> map = getMap(key);
		return map == null ? null : map.get(field);
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
	public void setExpireAt(String key, Date expireAt) {
		Object value = cache.getIfPresent(key);
		if(value == null)return;
		if(value instanceof ExpireableObject) {
			((ExpireableObject)value).setExpireAt(expireAt.getTime());
		}
		
	}

	@Override
	public long getTtl(String key) {
		Object value = cache.getIfPresent(key);
		if(value == null)return 0;
		if(value instanceof ExpireableObject) {
			long diff = ((ExpireableObject)value).getExpireAt() - System.currentTimeMillis();
			return diff < 0 ? 0 : diff/1000;
		}
		return -1;
	}

}
