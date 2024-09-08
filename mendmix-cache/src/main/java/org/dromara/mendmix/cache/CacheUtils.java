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
package org.dromara.mendmix.cache;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import org.dromara.mendmix.cache.adapter.LocalCacheAdapter;
import org.dromara.mendmix.cache.adapter.RedisCacheAdapter;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.async.ICaller;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.spring.InstanceFactory;

/**
 * 
 * <br>
 * Class Name   : CacheUtils
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年7月29日
 */
public class CacheUtils {
	
	private static final String PATH_SPEC_CHARS = ":/";
	public static final String CTX_IGNORE_TENANT_STRATEGY = "cache_ignore_tenant_strategy";
	public static final String CTX_IGNORE_PREFIX_STRATEGY = "cache_ignore_prefix_strategy";
	private static boolean redis;
	private static CacheAdapter cacheAdapter;
	private static File localFileCacheDir;
	
	static {
		String dataDir = ResourceUtils.getProperty("mendmix-cloud.data.dir");
		if(StringUtils.isNotBlank(dataDir)) {
			try {
				File dir = new File(dataDir,"cache");
				boolean exists;
				if(!(exists = dir.exists())) {
					exists = dir.mkdirs();
				}
				if(exists) {
					localFileCacheDir = dir;
					System.out.println(">>>>>> application.data.dir = " + dataDir);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static boolean isRedis() {
		getCacheAdapter();
		return redis;
	}

	public static CacheAdapter getCacheAdapter() {
		if(cacheAdapter !=  null)return cacheAdapter;
		synchronized (CacheUtils.class) {
			if(cacheAdapter !=  null)return cacheAdapter;
			if(ResourceUtils.containsAnyProperty("spring.redis.host","spring.redis.sentinel.nodes","spring.redis.cluster.nodes")) {
				cacheAdapter = new RedisCacheAdapter();
				redis = true;
			}else {
				cacheAdapter = new LocalCacheAdapter(3600);
			}
		}
		return cacheAdapter;
	}

	public static <T> T queryTryCache(String entityName,Object query,ICaller<T> dataCaller){
		return queryTryCache(entityName, query, dataCaller, CacheExpires.todayEndSeconds());
	}

	public static <T> T queryTryCache(String entityName,Object query,ICaller<T> dataCaller,long expireSeconds){
		String cacheKey = CacheKeyBuilder.buildCacheKey(entityName, query);
		T result = getCacheAdapter().get(cacheKey);
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
	
	public static <T> T queryTryCache(String cacheKey,ICaller<T> dataCaller,long expireSeconds){
		T result = getCacheAdapter().get(cacheKey);
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

	public static <T,R> T queryTryCache(String namespace, R identity, Function<R, T> dataFetcher, long expireSeconds){
		String cacheKey = String.format("%s:%s", namespace, identity);
		T result = getCacheAdapter().get(cacheKey);
		if(result == null){
			try {
				result = dataFetcher.apply(identity);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if(result != null){
				cacheAdapter.set(cacheKey, result, expireSeconds);
			}
		}
		return result;
	}
	
	public static <T> T queryLevel1Cache(String cacheKey) {
		T result = null;
		if(redis) {
			result = LocalCacheAdapter.miniteCache().get(cacheKey);
		}
		if(result == null ) {
			result = get(cacheKey);
			if(redis && result != null) {
				//本地缓存一分钟
				LocalCacheAdapter.miniteCache().set(cacheKey, result, 60);
			}
		}
		return result;
	}
	
	public static <T> T queryTryLevel1Cache(String cacheKey,ICaller<T> dataCaller,long expireSeconds){
		T result = queryLevel1Cache(cacheKey);
		if(result == null){
			try {
				synchronized (CacheUtils.class) {
					result = LocalCacheAdapter.hourCache().get(cacheKey);
					if(result != null)return result;
					result = dataCaller.call();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if(result != null){
				if(redis) {
					LocalCacheAdapter.miniteCache().set(cacheKey, result, 60);
				}
				set(cacheKey, result, expireSeconds);
		    }
		}
		return result;
	}
	
	@Deprecated
	public static <T> T queryTryLocalCache(String cacheKey,ICaller<T> dataCaller,long expireSeconds){
		return queryTryLevel1Cache(cacheKey, dataCaller, expireSeconds);
	}
	
	public static <T> T get(String key) {
		return getCacheAdapter().get(key);
	}
		
	public static String getStr(String key) {
		return getCacheAdapter().getStr(key);
	}
		
	public static void set(String key,Object value,long expireSeconds) {
		getCacheAdapter().set(key, value, expireSeconds);
	}
		
	public static void setStr(String key,String value,long expireSeconds) {
		getCacheAdapter().setStr(key, value, expireSeconds);
	}
		
	public static void remove(String...keys) {
		getCacheAdapter().remove(keys);
	}
		
	public static boolean exists(String key) {
		return getCacheAdapter().exists(key);
	}
		
	public static long size(String key) {
		return getCacheAdapter().size(key);
	}
		
	public static void setExpire(String key,long expireSeconds) {
		getCacheAdapter().setExpire(key, expireSeconds);
	}
		
	public static boolean setIfAbsent(String key, Object value,long timeout,TimeUnit timeUnit) {
		return getCacheAdapter().setIfAbsent(key, value, timeout, timeUnit);
	}
	
	public static void addStrItemToList(String key, String item) {
		getCacheAdapter().addStrItemToList(key, item);
	}

	public static List<String> getStrListItems(String key, int start, int end) {
		return getCacheAdapter().getStrListItems(key, start, end);
	}

	public static long getListSize(String key) {
		return getCacheAdapter().getListSize(key);
	}
	
	public static void removeListValue(String key,String value) {
		getCacheAdapter().removeListValue(key, value);
	}
	
	public static long getExpireIn(String key,TimeUnit timeUnit) {
		return getCacheAdapter().getExpireIn(key, timeUnit);
	}
	
	public static void setMapValue(String key, String field, Object value) {
		getCacheAdapter().setMapValue(key, field, value);
	}
	
	public static void setMapValues(String key, Map<String,Object> map){
		getCacheAdapter().setMapValues(key, map);
	}


	public static <T> T getMapValue(String key, String field) {
		return getCacheAdapter().getMapValue(key, field);
	}

	public static <T> Map<String, T> getMapValues(String key, Collection<String> fields) {
		return getCacheAdapter().getMapValues(key, fields);
	}
	
	public static void removeMapValue(String key, String field) {
		getCacheAdapter().removeMapValue(key, field);
	}
	
	public static boolean hasMapValue(String key, String field) {
		return getCacheAdapter().hasMapValue(key, field);
	}

	public static Set<String> getKeys(String pattern) {
		return getCacheAdapter().getKeys(pattern);
	}
	
	public static void setMapStringValue(String key, String field, String value) {
		getCacheAdapter().setMapStringValue(key, field, value);
	}

	public static void setMapStringValues(String key, Map<String, String> map) {
		getCacheAdapter().setMapStringValues(key, map);
	}

	public static String getMapStringValue(String key, String field) {
		return getCacheAdapter().getMapStringValue(key, field);
	}

	public static Map<String, String> getMapStringValues(String key, Collection<String> fields) {
		return getCacheAdapter().getMapStringValues(key, fields);
	}
	
	public static <T> Map<String, T> getMapValues(String key) {
		return getCacheAdapter().getMapValues(key);
	}
	
	public static Map<String, String> getMapStringValues(String key) {
		return getCacheAdapter().getMapStringValues(key);
	}
	
	public static String setFallbackCache(String key,String value,long expireSeconds) {
		if(InstanceFactory.isLoadfinished() && isRedis()) {
			getCacheAdapter().setStr(key, value, expireSeconds);
		}
		//
		if(localFileCacheDir != null) {
			//避免key包含路径分隔符
			String formatKey = StringUtils.replaceChars(key, PATH_SPEC_CHARS, GlobalConstants.UNDER_LINE);
			File cacheFile = new File(localFileCacheDir, formatKey);
			try {
				FileUtils.write(cacheFile, value, StandardCharsets.UTF_8);
				return cacheFile.getAbsolutePath();
			} catch (IOException e) {
				System.err.println(">>> write local cache:["+key+"]error");
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static String getFallbackCache(String key) {
		String value = null;
		if(InstanceFactory.isLoadfinished() && isRedis()) {
			value = getCacheAdapter().getStr(key);
		}
		if(StringUtils.isNotBlank(value) || localFileCacheDir == null) {
			return value;
		}
		try {
			String formatKey = StringUtils.replaceChars(key, PATH_SPEC_CHARS, GlobalConstants.UNDER_LINE);
			File cacheFile = new File(localFileCacheDir, formatKey);
			if (cacheFile.exists()) {
				value = FileUtils.readFileToString(cacheFile, StandardCharsets.UTF_8);
			} else if(!GlobalContext.isStarting()) {
				System.err.println(">>> read local cache error ,cacheFile["+cacheFile.getAbsolutePath()+"] not exists");
			}
		} catch (Exception e) {
			System.err.println(">>> read local cache:["+key+"]error");
			e.printStackTrace();
		}
		return value;
	}

}
