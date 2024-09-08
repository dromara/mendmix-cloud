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
package org.dromara.mendmix.cache.command;

import static org.dromara.mendmix.cache.redis.JedisProviderFactory.getJedisCommands;
import static org.dromara.mendmix.cache.redis.JedisProviderFactory.getJedisProvider;

import java.util.List;

public class RedisStrList extends RedisBase {

	private long expireTime;//过期时间（秒）
	
	public RedisStrList(String key) {
		this(key, null, RedisBase.getDefaultExpireSeconds());
	}
	/**
	 * 
	 * @param key
	 * @param groupName 组名
	 */
	public RedisStrList(String key,String groupName) {
		this(key, groupName, RedisBase.getDefaultExpireSeconds());
	}
	
	public RedisStrList(String key,String groupName,long expireTime) {
		super(key,groupName,false);
		this.expireTime = expireTime;
	}
	

	public boolean lpush(String...strings) {
		try {
			boolean result = getJedisCommands(groupName).lpush(key, strings) == 1;
			//设置超时时间
			if(result)setExpireIfNot(expireTime);
			return result;
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	public boolean rpush(String...strings) {
		try {
			boolean result = getJedisCommands(groupName).rpush(key, strings) == 1;
			//设置超时时间
			if(result)setExpireIfNot(expireTime);
			return result;
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	public String lpop() {
		try {
			return getJedisCommands(groupName).lpop(key);
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	public String rpop() {
		try {
			return getJedisCommands(groupName).rpop(key);
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	/**
	 * 获取全部列表
	 * 
	 * @return
	 */
	public List<String> get() {
		return range(0, -1);
	}

	public List<String> range(int start, int end) {
		try {
			return getJedisCommands(groupName).lrange(key, start, end);
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	/**
	 * 返回长度
	 * 
	 * @return
	 */
	public long length() {
		try {
			return getJedisCommands(groupName).llen(key);
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	/**
	 * 设置指定位置的值
	 * 
	 * @param index
	 * @param newValue
	 * @return
	 */
	public boolean set(long index, String newValue) {
		try {
			return getJedisCommands(groupName).lset(key, index, newValue).equals(RESP_OK);
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	/**
	 * 移除(所有)指定值元素
	 * @param value
	 * @return
	 */
	public boolean removeValue(String value) {
		try {
			return getJedisCommands(groupName).lrem(key, 0, value) >= 1;
		} finally {
			getJedisProvider(groupName).release();
		}
	}

}
