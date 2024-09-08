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

import java.util.Date;

import org.dromara.mendmix.cache.local.Level1CacheSupport;

/**
 * 字符串redis操作命令
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public class RedisString extends RedisBase{
	
	public RedisString(String key) {
		super(key, false);
	}
	
	/**
	 * 
	 * @param key
	 * @param groupName 组名
	 */
	public RedisString(String key,String groupName) {
		super(key, groupName, false);
	}

	/**
	 * 重置key（适合一个方法里面频繁操作不同缓存的场景）<br>
	 * <font color="red">非线程安全，请不要在多线程场景使用</font>
	 * 
	 * @param key
	 * @return
	 */
	public RedisString resetKey(String key) {
		this.key = key;
		return this;
	}
	
	/**
	 * 设置缓存，默认过期时间
	 * @param value
	 * @return
	 */
	public boolean set(String value){
		return set(value, RedisBase.getDefaultExpireSeconds());
	}
	
	/**
	 * 设置缓存指定过期时间间隔
	 * @param value
	 * @param seconds (过期秒数 ，小于等于0时 不设置)
	 * @return
	 */
	public boolean set(String value, long seconds) {

		if (value == null)
			return false;
		try {
			boolean result = getJedisCommands(groupName).set(key, value).equals(RESP_OK);
			if(result && seconds > 0){
				result =  setExpire(seconds);
				//set可能是更新缓存，所以统一通知各节点清除本地缓存
				Level1CacheSupport.getInstance().publishSyncEvent(key);
			}
			return result;
		} finally {
			getJedisProvider(groupName).release();
		}
		
	}

	/**
	 * 检查给定 key 是否存在。
	 * @return
	 */
	public boolean set(String value, Date expireAt) {
		if (value == null)
			return false;
		try {
			boolean result = getJedisCommands(groupName).set(key, value).equals(RESP_OK);
			if(result){
				result = setExpireAt(expireAt);
				//set可能是更新缓存，所以统一通知各节点清除本地缓存
				Level1CacheSupport.getInstance().publishSyncEvent(key);
			}
			return result;
		} finally {
			getJedisProvider(groupName).release();
		}
	}
	
	public String get() {
		String value = Level1CacheSupport.getInstance().get(key);
		if(value != null)return value;
		try {
			value = getJedisCommands(groupName).get(key);
			return value;
		} finally {
			getJedisProvider(groupName).release();
			//
			Level1CacheSupport.getInstance().set(key, value);
		}
		
	}

	/**
	 * 删除给定的一个 key 。
	 * @return true：存在该key删除时返回
	 * 
	 *         false：不存在该key
	 */
	public boolean remove() {
		boolean removed = super.remove();
		//通知清除本地缓存
		if(removed)Level1CacheSupport.getInstance().publishSyncEvent(key);
		return removed;
	}


	public boolean setnx(String value, long expireSeconds) {
		try {
			Long result = getJedisCommands(groupName).setnx(key, value);
			if(result > 0)setExpire(expireSeconds);
			return result > 0;
		} finally {
			getJedisProvider(groupName).release();
		}
	}
}
