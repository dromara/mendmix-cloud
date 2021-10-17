package com.jeesuite.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.cache.command.RedisObject;
import com.jeesuite.cache.redis.JedisProviderFactory;
import com.jeesuite.security.SecurityConstants.CacheType;
import com.jeesuite.security.cache.LocalCache;
import com.jeesuite.security.cache.RedisCache;
import com.jeesuite.security.model.ExpireableObject;

public class SecurityStorageManager {
	
	private static Logger log = LoggerFactory.getLogger("com.jeesuite.security");
	
	protected static final String CACHE_GROUP_NAME = "security";
	
    private Map<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
	
	private boolean sharingSession;
	
	private Cache localTempCache;
	
	protected SecurityStorageManager(CacheType cacheType) {
		if(sharingSession = cacheType == CacheType.redis){
			JedisProviderFactory.addGroupProvider(CACHE_GROUP_NAME);
		}else {
			localTempCache = new LocalCache(15 * 60);
		}
	}

	protected void addCahe(String cacheName,int timeToLiveSeconds){
		Cache cache = null;
		if(sharingSession){
			cache = new RedisCache(CACHE_GROUP_NAME,cacheName,timeToLiveSeconds);
		}else{
			cache = new LocalCache(timeToLiveSeconds);
		}
		caches.put(cacheName, cache);
	}
	
	protected Cache getCache(String cacheName) {
		return caches.get(cacheName);
	}
	
	public void setTemporaryCacheValue(String key, Object value, int expireInSeconds) {
		if(sharingSession) {
			new RedisObject(key,CACHE_GROUP_NAME).set(value, expireInSeconds);
		}else {
			ExpireableObject expireableObject = new ExpireableObject(value, System.currentTimeMillis() + expireInSeconds * 1000);
			localTempCache.setObject(key, expireableObject);
		}
	}
    
    public <T> T getTemporaryCacheValue(String key) {
		Object obj = null;
		if(sharingSession) {
			obj = new RedisObject(key,CACHE_GROUP_NAME).get();
		}else {
			ExpireableObject expireableObject = (ExpireableObject) localTempCache.getObject(key);
			if(expireableObject != null && expireableObject.getExpireAt() >= System.currentTimeMillis()) {
				obj = expireableObject.getTarget();
			}
		}
		return (T) obj;
	}
}
