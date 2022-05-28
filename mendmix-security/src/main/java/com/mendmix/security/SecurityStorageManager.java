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
package com.mendmix.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.cache.command.RedisObject;
import com.mendmix.cache.redis.JedisProviderFactory;
import com.mendmix.security.SecurityConstants.CacheType;
import com.mendmix.security.cache.LocalCache;
import com.mendmix.security.cache.RedisCache;
import com.mendmix.security.model.ExpireableObject;

public class SecurityStorageManager {
	
	private static Logger log = LoggerFactory.getLogger("com.mendmix.security");
	
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
