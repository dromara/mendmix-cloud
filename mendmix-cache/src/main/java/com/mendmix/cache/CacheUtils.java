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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.mendmix.cache.adapter.LocalCacheAdapter;
import com.mendmix.cache.adapter.RedisCacheAdapter;
import com.mendmix.common.GlobalConstants;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.async.ICaller;
import com.mendmix.common.util.ResourceUtils;

public class CacheUtils {
	
	private static final String PATH_SPEC_CHARS = ":/";
	
	private static boolean redis;
	private static CacheAdapter cacheAdapter;
	private static File localFileCacheDir;
	
	static {
		if(ResourceUtils.containsAnyProperty("spring.redis.host","spring.redis.sentinel.nodes","spring.redis.cluster.nodes")) {
			cacheAdapter = new RedisCacheAdapter();
			redis = true;
		}else {
			cacheAdapter = new LocalCacheAdapter(3600);
		}
		if(GlobalRuntimeContext.getAppDataDir() != null) {
			localFileCacheDir = new File(GlobalRuntimeContext.getAppDataDir(), "cache");
			if(!localFileCacheDir.exists())localFileCacheDir.mkdirs();
		}
	}
	
	public static boolean isRedis() {
		return redis;
	}

	public static CacheAdapter getCacheAdapter() {
		return cacheAdapter;
	}
	
	public static <T> T queryTryCache(String cacheKey,ICaller<T> dataCaller,long expireSeconds){
		T result = cacheAdapter.get(cacheKey);
		if(result == null){
			try {
				result = dataCaller.call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if(result != null){
				cacheAdapter.set(cacheKey, result, expireSeconds);
		    }
		}
		return result;
	}
	
	
	public static <T> T get(String key) {
		return cacheAdapter.get(key);
	}
		
	public static String getStr(String key) {
		return cacheAdapter.getStr(key);
	}
		
	public static void set(String key,Object value,long expireSeconds) {
		cacheAdapter.set(key, value, expireSeconds);
	}
		
	public static void setStr(String key,String value,long expireSeconds) {
		cacheAdapter.setStr(key, value, expireSeconds);
	}
		
	public static void remove(String...keys) {
		cacheAdapter.remove(keys);
	}
		
	public static boolean exists(String key) {
		return cacheAdapter.exists(key);
	}
		
	public static long size(String key) {
		return cacheAdapter.size(key);
	}
		
	public static void setExpire(String key,long expireSeconds) {
		cacheAdapter.setExpire(key, expireSeconds);
	}
		
	public static boolean setIfAbsent(String key, Object value,long timeout,TimeUnit timeUnit) {
		return cacheAdapter.setIfAbsent(key, value, timeout, timeUnit);
	}
	
	public static void addStrItemToList(String key, String item) {
		cacheAdapter.addStrItemToList(key, item);
	}

	public static List<String> getStrListItems(String key, int start, int end) {
		return cacheAdapter.getStrListItems(key, start, end);
	}

	public static long getListSize(String key) {
		return cacheAdapter.getListSize(key);
	}
	
	public static long getExpireIn(String key,TimeUnit timeUnit) {
		return cacheAdapter.getExpireIn(key, timeUnit);
	}
	
	public static void setMapValue(String key, String field, Object value) {
		cacheAdapter.setMapValue(key, field, value);
	}
	
	public static void setMapValues(String key, Map<String,Object> map){
		cacheAdapter.setMapValues(key, map);
	}


	public static <T> T getMapValue(String key, String field) {
		return cacheAdapter.getMapValue(key, field);
	}

	public static <T> Map<String, T> getMapValues(String key, Collection<String> fields) {
		return cacheAdapter.getMapValues(key, fields);
	}

	public static Set<String> getKeys(String pattern) {
		return cacheAdapter.getKeys(pattern);
	}
	
	public static String setLocalFallbackCache(String key,String value,long expireSeconds) {
		if(!GlobalRuntimeContext.isStarting()) {
			cacheAdapter.setStr(key, value, expireSeconds);
		}else if(localFileCacheDir != null) {
			//避免key包含路径分隔符
			String formatKey = StringUtils.replaceChars(key, PATH_SPEC_CHARS, GlobalConstants.UNDER_LINE);
			File cacheFile = new File(localFileCacheDir, formatKey);
			try {
				FileUtils.write(cacheFile, value, StandardCharsets.UTF_8.name());
				return cacheFile.getAbsolutePath();
			} catch (IOException e) {
				System.err.println(">>> write local cache:["+key+"]error");
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static String getLocalFallbackCache(String key) {
		if(!GlobalRuntimeContext.isStarting()) {
			return cacheAdapter.getStr(key);
		}else if(localFileCacheDir != null) {
			String value = null;
			try {
				String formatKey = StringUtils.replaceChars(key, PATH_SPEC_CHARS, GlobalConstants.UNDER_LINE);
				File cacheFile = new File(localFileCacheDir, formatKey);
				if (cacheFile.exists()) {
					value = FileUtils.readFileToString(cacheFile, StandardCharsets.UTF_8);
				}
			} catch (Exception e) {
				System.err.println(">>> read local cache:["+key+"]error");
				e.printStackTrace();
			}
			return value;
		}
		return null;
		
	}
	
}
