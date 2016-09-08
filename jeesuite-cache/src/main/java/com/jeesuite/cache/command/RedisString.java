/**
 * 
 */
package com.jeesuite.cache.command;

import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisProvider;

import java.util.Date;

import com.jeesuite.cache.local.LocalCacheProvider;
import com.jeesuite.cache.local.LocalCacheSyncProcessor;

/**
 * 字符串redis操作命令
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public class RedisString {
	
	protected static final String RESP_OK = "OK";

	protected String key;
	
	protected String groupName;

	
	public RedisString(String key) {
		this.key = key;
		if(key.contains(RedisBase.KEY_SUFFIX_SPLIT)){
			this.groupName = key.split(RedisBase.KEY_SUFFIX_SPLIT)[0];
		}
	}
	
	/**
	 * 
	 * @param key
	 * @param groupName 组名
	 */
	public RedisString(String key,String groupName) {
		this.key = key;
		this.groupName = groupName;
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
				LocalCacheSyncProcessor.getInstance().publishSyncEvent(key);
			}
			return result;
		} finally {
			getJedisProvider(groupName).release();
		}
		
	}

	/**
	 * 检查给定 key 是否存在。
	 * 
	 * @param key
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
				LocalCacheSyncProcessor.getInstance().publishSyncEvent(key);
			}
			return result;
		} finally {
			getJedisProvider(groupName).release();
		}
	}
	
	public String get() {
		String value = LocalCacheProvider.getInstance().get(key);
		if(value != null)return value;
		try {
			value = getJedisCommands(groupName).get(key);
			return value;
		} finally {
			getJedisProvider(groupName).release();
			//
			LocalCacheProvider.getInstance().set(key, value);
		}
		
	}


	/**
	 * 检查给定 key 是否存在。
	 * 
	 * @param key
	 * @return
	 */
	public boolean exists() {
		try {
			return getJedisCommands(groupName).exists(key);
		} finally {
			getJedisProvider(groupName).release();
		}

	}

	/**
	 * 删除给定的一个 key 。
	 * 
	 * 不存在的 key 会被忽略。
	 * 
	 * @param key
	 * @return true：存在该key删除时返回
	 * 
	 *         false：不存在该key
	 */
	public boolean remove() {
		try {
			return getJedisCommands(groupName).del(key) == 1;
		} finally {
			getJedisProvider(groupName).release();
			//
			LocalCacheSyncProcessor.getInstance().publishSyncEvent(key);
		}
	}

	/**
	 * 为给定 key 设置生存时间，当 key 过期时(生存时间为 0 )，它会被自动删除。
	 * 
	 * @param key
	 * @param seconds
	 *            超时时间，单位：秒
	 * @return true：超时设置成功
	 * 
	 *         false：key不存在或超时未设置成功
	 */
	public boolean setExpire(long seconds) {
		try {
			return getJedisCommands(groupName).pexpire(key, seconds * 1000) == 1;
		} finally {
			getJedisProvider(groupName).release();
		}

	}

	/**
	 * 
	 * 设置指定时间戳时失效
	 *
	 * 注意：redis服务器时间问题
	 * 
	 * @param key
	 * @param expireAt
	 *            超时时间点
	 * @return true：超时设置成功
	 *
	 *         false：key不存在或超时未设置成功
	 */
	public boolean setExpireAt(Date expireAt) {
		try {
			return getJedisCommands(groupName).pexpireAt(key, expireAt.getTime()) == 1;
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	/**
	 * 返回给定 key 的剩余生存时间(单位：秒)
	 * 
	 * @param key
	 * @return 当 key 不存在时，返回 -2 。
	 * 
	 *         当 key 存在但没有设置剩余生存时间时，返回 -1 。
	 * 
	 *         否则，以毫秒为单位，返回 key的剩余生存时间。
	 */
	public Long getTtl() {
		try {
			return getJedisCommands(groupName).ttl(key);
		} finally {
			getJedisProvider(groupName).release();
		}

	}

	/**
	 * 移除给定 key 的生存时间，设置为永久有效
	 * 
	 * @param key
	 * @return 当生存时间移除成功时，返回 1 .
	 * 
	 *         如果 key 不存在或 key 没有设置生存时间，返回 0 。
	 */
	public boolean removeExpire() {
		try {
			return getJedisCommands(groupName).persist(key) == 1;
		} finally {
			getJedisProvider(groupName).release();
		}
	}
}
