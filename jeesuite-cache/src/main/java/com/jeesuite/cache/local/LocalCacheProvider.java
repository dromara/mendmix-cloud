/**
 * 
 */
package com.jeesuite.cache.local;

import java.util.List;
import java.util.concurrent.Callable;
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
public class LocalCacheProvider {
	
	private static final String _NULL = "NULL";

	private static final Logger logger = LoggerFactory.getLogger(LocalCacheProvider.class);
	
	private List<String> keyPrefixs;
	
	private static LocalCacheProvider instance = new LocalCacheProvider();
	
	private Cache<String, Object> cache;
	
	
	public static LocalCacheProvider getInstance() {
		return instance;
	}

	protected static void init(List<String> keyPrefixs,long maxSize,long expireMins) {
		instance = new LocalCacheProvider();
		instance.keyPrefixs = keyPrefixs;
		instance.cache = CacheBuilder
		          .newBuilder()
		          .maximumSize(maxSize)
		          .expireAfterWrite(expireMins, TimeUnit.MINUTES)
		          .build();
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
					logger.debug("get cache:{} from local",key);
					return (T)result;
				}
			}
		} catch (Exception e) {
			logger.warn("get local cache error",e);
		}
		return null;
	}
	
	public void remove(String key){
		Cache<String, Object> cache = getCacheHolder(key);
		if(cache != null){
			cache.invalidate(key);
			logger.debug("remove local cache:{}",key);
		}
	}
	
	private Cache<String, Object> getCacheHolder(String key){
		if(keyPrefixs == null)return null;
		for (String prefix : keyPrefixs) {
			if(key.indexOf(prefix) == 0)return cache;
		}
		return null;
	}
	
	public void clearAll(){
		cache.invalidateAll();
	}
}
