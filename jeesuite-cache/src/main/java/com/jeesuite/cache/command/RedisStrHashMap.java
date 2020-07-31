/**
 * 
 */
package com.jeesuite.cache.command;

import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public class RedisStrHashMap extends RedisBase {

private long expireTime;//过期时间（秒）
	
	public RedisStrHashMap(String key) {
		this(key, null, RedisBase.getDefaultExpireSeconds());
	}
	/**
	 * 
	 * @param key
	 * @param groupName 组名
	 */
	public RedisStrHashMap(String key,String groupName) {
		this(key, groupName, RedisBase.getDefaultExpireSeconds());
	}
	
	public RedisStrHashMap(String key,String groupName,long expireTime) {
		super(key,groupName,false);
		this.expireTime = expireTime;
	}
	
	/**
	 * 设置hash缓存
	 * 
	 * @param datas
	 * @return
	 */
	public boolean set(Map<String, String> datas) {
		if(datas == null || datas.isEmpty())return false;
		boolean result = false;
		try {
			result = getJedisCommands(groupName).hmset(key, datas).equals(RESP_OK);
			//设置超时时间
			if(result)setExpireIfNot(expireTime);
			return result;
		} finally {
			getJedisProvider(groupName).release();
		}

	}
	
	/**
	 * 获取所有值
	 * 
	 * @return
	 */
	public Map<String, String> getAll() {
		try {
			return getJedisCommands(groupName).hgetAll(key);
		} finally {
			getJedisProvider(groupName).release();
		}

	}

	/**
	 * 查看缓存hash是否包含某个key
	 * 
	 * @param field
	 * @return
	 */
	public boolean containsKey(String field) {
		try {
			return getJedisCommands(groupName).hexists(key, field);
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	/**
	 * 设置ç
	 * 
	 * @param field
	 * @param value
	 * @return
	 */
	public boolean set(String field, String value) {
		boolean result = false;
		if(value == null)return false;
		//返回值（1:新字段被设置,0:已经存在值被更新）
		try {
			result = getJedisCommands(groupName).hset(key, field, value) >= 0;
			//设置超时时间
			if(result)setExpireIfNot(expireTime);
			return result;
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	/**
	 * 移除hash缓存中的指定值
	 * 
	 * @param field
	 * @return
	 */
	public boolean remove(String field) {
		try {
			return getJedisCommands(groupName).hdel(key, field).equals(RESP_OK);
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
			return getJedisCommands(groupName).hlen(key);
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	/**
	 * 获取一个值
	 * 
	 * @param field
	 * @return
	 */
	public String getOne(String field) {
		return get(field).get(field);
	}

	/**
	 * 获取多个key的值
	 * 
	 * @param fields
	 * @return
	 */
	public Map<String, String> get(String... fields) {
		try {
			List<String> datas = getJedisCommands(groupName).hmget(key, fields);
			Map<String, String> result = new HashMap<>();
			
			for (int i = 0; i < fields.length; i++) {
				result.put(fields[i], datas.get(i));
			}
			return result;
		} finally {
			getJedisProvider(groupName).release();
		}

	}
	
	public long incr(String field,long value) {
		try {
			return getJedisCommands(groupName).hincrBy(key, field, value);
		} finally {
			getJedisProvider(groupName).release();
		}

	}
}
