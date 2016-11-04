/**
 * 
 */
package com.jeesuite.cache.local;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * 本地缓存服务
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月1日
 */
public class Level1CacheProvider {
	
	private static final String _NULL = "NULL";

	private static final Logger logger = LoggerFactory.getLogger(Level1CacheProvider.class);
	
	private int maxSize;
	private int timeToLiveSeconds;
	private List<String> cacheNames;
	
	private static Level1CacheProvider instance = new Level1CacheProvider();
	
	private Map<String, Cache<String, Object>> caches = new ConcurrentHashMap<String, Cache<String,Object>>();
	
	
	public static Level1CacheProvider getInstance() {
		return instance;
	}

	protected static void init(List<String> cacheNames,int maxSize,int timeToLiveSeconds) {
		instance = new Level1CacheProvider();
		instance.cacheNames = cacheNames;
		instance.maxSize = maxSize;
		instance.timeToLiveSeconds = timeToLiveSeconds;
	}

	public boolean set(String key,Object value){
		if(value == null)return true;
		Cache<String, Object> cache = getCacheHolder(key);
		if(cache != null){
			cache.put(key, value);
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String key){
		try {			
			Cache<String, Object> cache = getCacheHolder(key);
			if(cache != null){
				Object result = cache.get(key, new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						return _NULL;
					}
				});
				if(result != null && !_NULL.equals(result)){
					logger.debug("get cache:{} from LEVEL1",key);
					return (T)result;
				}
			}
		} catch (Exception e) {
			logger.warn("get LEVEL1 cache error",e);
		}
		return null;
	}
	
	public void remove(String key){
		Cache<String, Object> cache = getCacheHolder(key);
		if(cache != null){
			cache.invalidateAll();
			if(logger.isDebugEnabled())logger.debug("remove LEVEL1 cache:{}",key.split("\\.")[0]);
		}
	}
	
	private Cache<String, Object> getCacheHolder(String key){
		if(cacheNames == null)return null;
		String cacheName = key.split("\\.")[0];
		if(!cacheNames.contains(cacheName))return null;
		return getAndNotexistsCreateCache(cacheName);
	}
	
	private Cache<String, Object> getAndNotexistsCreateCache(String cacheName){
		Cache<String, Object> cache = caches.get(cacheName);
		if(cache != null)return cache;
		cache = CacheBuilder
		          .newBuilder()
		          .maximumSize(maxSize)
		          .expireAfterWrite(timeToLiveSeconds, TimeUnit.SECONDS)
		          .build();
		
		caches.put(cacheName, cache);
		
		return cache;
		
	}
	
	public void clearAll(){
		for (Cache<String, Object> cache : caches.values()) {
			cache.invalidateAll();
		}
	}
}
