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
package org.dromara.mendmix.cache.local;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月4日
 */
public class EhCacheLevel1CacheProvider implements Level1CacheProvider {

	private CacheManager manager;
	private Map<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
	
	private String ehcacheName;
	
	public void setEhcacheName(String ehcacheName) {
		this.ehcacheName = ehcacheName;
	}

	@Override
	public void start() {
		if (ehcacheName != null)
			manager = CacheManager.getCacheManager(ehcacheName);
		if (manager == null) {
		    manager = new CacheManager();
		}
	}
	
	@Override
	public void close() throws IOException {
		manager.shutdown();
	}

	@Override
	public boolean set(String cacheName, String key, Object value) {
		getCacheHolder(cacheName).put(new Element( key, value ));
		return true;
	}

	@Override
	public <T> T get(String cacheName, String key) {
		Element element = getCacheHolder(cacheName).get(key);
		if(element == null)return null;
		return (T) element.getObjectValue();
	}

	@Override
	public void remove(String cacheName, String key) {
		getCacheHolder(cacheName).remove(key);
	}

	@Override
	public void remove(String cacheName) {
		getCacheHolder(cacheName).removeAll();
	}

	@Override
	public void clearAll() {
		for (Cache cache : caches.values()) {
			cache.removeAll();
		}
	}
	
	private Cache getCacheHolder(String cacheName){
		return getAndNotexistsCreateCache(cacheName);
	}
	
	private Cache getAndNotexistsCreateCache(String cacheName){
		Cache cache = caches.get(cacheName);
		if(cache != null)return cache;
		synchronized (caches) {
			if((cache = caches.get(cacheName)) != null)return cache;
			manager.addCache(cacheName);
			cache = manager.getCache(cacheName);
			caches.put(cacheName, cache);
		}
		return cache;	
	}

}
