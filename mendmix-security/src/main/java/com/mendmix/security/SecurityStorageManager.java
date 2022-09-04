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

import com.mendmix.security.SecurityConstants.CacheType;
import com.mendmix.security.cache.LocalCache;
import com.mendmix.security.cache.RedisCache;

public class SecurityStorageManager {
	
	
    private Map<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
	
	private CacheType cacheType;
	
	protected SecurityStorageManager(CacheType cacheType) {
		this.cacheType = cacheType;
	}

	protected void addCahe(String cacheName,int timeToLiveSeconds){
		Cache cache = null;
		if(cacheType == CacheType.redis){
			cache = new RedisCache(cacheName,timeToLiveSeconds);
		}else{
			cache = new LocalCache(timeToLiveSeconds);
		}
		caches.put(cacheName, cache);
	}
	
	protected Cache getCache(String cacheName) {
		return caches.get(cacheName);
	}
	
}
