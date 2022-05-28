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
package com.mendmix.cache;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.mendmix.cache.adapter.LocalCacheAdapter;
import com.mendmix.spring.InstanceFactory;

public class CacheUtils {

	private static CacheAdapter cacheAdapter;
	
	
	public static void setCacheAdapter(CacheAdapter cacheAdapter) {
		CacheUtils.cacheAdapter = cacheAdapter;
	}

	public static CacheAdapter getCacheAdapter() {
		if(cacheAdapter == null) {
			synchronized (CacheUtils.class) {
				cacheAdapter = InstanceFactory.getInstance(CacheAdapter.class);
			}
			if(cacheAdapter == null) {
				cacheAdapter = new LocalCacheAdapter();
			}
		}
		return cacheAdapter;
	}

	public static <T> T get(String key) {
		return getCacheAdapter().get(key);
	}

	
	public static String getString(String key) {
		return getCacheAdapter().getString(key);
	}

	
	public static void set(String key, Object value, long expireSeconds) {
		getCacheAdapter().set(key, value, expireSeconds);
	}

	
	public static void setString(String key, Object value, long expireSeconds) {
		getCacheAdapter().setString(key, value, expireSeconds);
	}

	
	public static void remove(String... keys) {
		getCacheAdapter().remove(keys);
	}

	
	public static boolean exists(String key) {
		return getCacheAdapter().exists(key);
	}

	
	public static void addListItems(String key, String... items) {
		getCacheAdapter().addListItems(key, items);
	}

	
	public static List<String> getListItems(String key, int start, int end) {
		return getCacheAdapter().getListItems(key, start, end);
	}

	
	public static long getListSize(String key) {
		return getCacheAdapter().getListSize(key);
	}

	
	public static boolean setIfAbsent(String key, String value, long expireMillis) {
		return getCacheAdapter().setIfAbsent(key, value, expireMillis);
	}

	public Map<String, String> getMap(String key) {
		return getCacheAdapter().getMap(key);
	}
	
	public static void setMapItem(String key, String field, String value) {
		getCacheAdapter().setMapItem(key, field, value);
	}

	
	public static String getMapItem(String key, String field) {
		return getCacheAdapter().getMapItem(key, field);
	}

	
	public static void setExpire(String key, long expireSeconds) {
		getCacheAdapter().setExpire(key, expireSeconds);
	}

	
	public static void setExpireAt(String key, Date expireAt) {
		getCacheAdapter().setExpireAt(key, expireAt);
	}

	
	public static long getTtl(String key) {
		return getCacheAdapter().getTtl(key);
	}
}
