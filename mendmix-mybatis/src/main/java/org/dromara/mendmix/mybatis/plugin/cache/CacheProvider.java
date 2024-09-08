/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.mybatis.plugin.cache;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.dromara.mendmix.cache.CacheUtils;
import org.dromara.mendmix.cache.RedisTemplateGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月13日
 */
public class CacheProvider {

	protected static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");
	
	protected String groupName;

	protected int batchSize = 100;
	
	
	
	public CacheProvider(String groupName) {
		this.groupName = groupName;
	}

	
	public <T> T get(String key) {
		return CacheUtils.get(key);
	}


	
	public String getStr(String key) {
		return CacheUtils.getStr(key);
	}


	
	public void set(String key, Object value, long expireSeconds) {
		CacheUtils.set(key, value, expireSeconds);
	}


	
	public void setStr(String key, String value, long expireSeconds) {
		CacheUtils.setStr(key, value, expireSeconds);
	}


	
	public void remove(String... keys) {
		CacheUtils.remove(keys);
	}


	
	public boolean exists(String key) {
		return CacheUtils.exists(key);
	}


	
	public void setExpire(String key, long expireSeconds) {
		CacheUtils.setExpire(key, expireSeconds);
	}



	
	public List<String> getListItems(String key, int start, int end) {
		return CacheUtils.getStrListItems(key, start, end);
	}


	
	public long getListSize(String key) {
		return CacheUtils.getListSize(key);
	}


	
	public boolean setnx(String key, String value, long expireSeconds) {
		return CacheUtils.setIfAbsent(key, value, expireSeconds,TimeUnit.SECONDS);
	}
	
	
	public void putGroup(String cacheGroupKey, String key) {
		CacheUtils.addStrItemToList(cacheGroupKey, key);
	}
	
	
	public void clearGroup(final String groupName,String ...prefixs) {
		String cacheGroupKey = groupName.endsWith(CacheHandler.GROUPKEY_SUFFIX) ? groupName : groupName + CacheHandler.GROUPKEY_SUFFIX;
		int keyCount = (int) getListSize(cacheGroupKey);
		if(keyCount <= 0)return;

		boolean withPrefixs = prefixs != null && prefixs.length > 0 && prefixs[0] != null;
		
		int toIndex;
		List<String> keys;
		for (int i = 0; i <= keyCount; i+=batchSize) {
			toIndex = (i + batchSize) > keyCount ? keyCount : (i + batchSize);
			keys = getListItems(cacheGroupKey,i, toIndex);
			if(keys.isEmpty())break;
			//
			if(withPrefixs) {
				keys = keys.stream().filter(key -> {
					for (String prefix : prefixs) {
						if(key.contains(prefix))return true;
					}
					return false;
				}).collect(Collectors.toList());
			}
			if(keys.isEmpty())continue;
			//
			remove(keys.toArray(new String[0]));
			//
			StringRedisTemplate redisTemplate = RedisTemplateGroups.getDefaultStringRedisTemplate();
			for (String key : keys) {
				redisTemplate.opsForList().remove(cacheGroupKey, 1, key);
			}
			if(logger.isDebugEnabled()) {
				logger.debug(">> auto_cache_process clearGroupKey finish -> group:{},keys:{}",groupName,Arrays.toString(keys.toArray()));
			}
		}
	}

}
