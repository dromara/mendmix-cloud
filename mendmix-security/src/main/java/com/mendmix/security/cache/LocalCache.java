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
package com.mendmix.security.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.mendmix.security.Cache;

@SuppressWarnings("unchecked")
public class LocalCache implements Cache{
	
	private com.google.common.cache.Cache<String,Object> cache;

	public LocalCache(int timeToLiveSeconds) {
		cache =  CacheBuilder
				.newBuilder()
				.maximumSize(10000)
				.expireAfterWrite(timeToLiveSeconds, TimeUnit.SECONDS)
				.build();
	}

	@Override
	public void setString(String key, String value) {
		cache.put(key, value);
	}

	@Override
	public String getString(String key) {
		return Objects.toString(getObject(key), null);
	}

	@Override
	public void setObject(String key, Object value) {
		cache.put(key, value);
	}

	
	@Override
	public <T> T getObject(String key) {
		return (T) cache.getIfPresent(key);
	}

	@Override
	public void remove(String key) {
		cache.invalidate(key);
	}

	@Override
	public boolean exists(String key) {
		return cache.getIfPresent(key) != null;
	}

	@Override
	public void removeAll() {
		cache.invalidateAll();
	}

	@Override
	public void setMapValue(String key,String field,Object value) {
		Map<String, Object> map = getObject(key);
		if(map == null){
			map = new HashMap<>(1);
			setObject(key, map);
		}
		map.put(field, value);
		
	}

	@Override
	public <T> T getMapValue(String key, String field) {
		Map<String, Object> map = getObject(key);
		if(map == null)return null;
		return (T) map.get(field);
	}
	
	@Override
	public void updateExpireTime(String key) {
		
	}

}
