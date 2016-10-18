/**
 * 
 */
package com.jeesuite.cache.command;

import static com.jeesuite.cache.redis.JedisProviderFactory.getBinaryJedisClusterCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getBinaryJedisCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisProvider;
import static com.jeesuite.cache.redis.JedisProviderFactory.isCluster;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.cache.CacheExpires;
import com.jeesuite.common.serializer.SerializeUtils;

import redis.clients.util.SafeEncoder;

/**
 * redis基础操作指令
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public abstract class RedisBase {
	
	protected static final Logger logger = LoggerFactory.getLogger(RedisBase.class);
	
	protected static final String KEY_SUFFIX_SPLIT = "::";
    //
	protected static final String RESP_OK = "OK";
	//
	//
    protected String groupName;
    
	protected byte[] key;
	
	protected String origKey;
	
	public byte[] getKey() {
		return key;
	}

	public RedisBase(String key) {
		this.origKey = key;
		//
		if(key.contains(KEY_SUFFIX_SPLIT)){
			this.groupName = key.split(KEY_SUFFIX_SPLIT)[0];
		}
		this.key = SafeEncoder.encode(key);
	}
	
	public RedisBase(String key,String groupName) {
		this.origKey = key;
		this.key = SafeEncoder.encode(key);
		this.groupName = groupName;
	}

	/**
	 * 检查给定 key 是否存在。
	 * 
	 * @param key
	 * @return
	 */
	public boolean exists() {
		try {
			if(isCluster(groupName)){
				return getBinaryJedisClusterCommands(groupName).exists(key);
			}
			return getBinaryJedisCommands(groupName).exists(key);
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
			if(isCluster(groupName)){
				return getBinaryJedisClusterCommands(groupName).del(key) == 1;
			}
			return getBinaryJedisCommands(groupName).del(key) == 1;
		} finally {
			getJedisProvider(groupName).release();
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
		if(seconds <= 0)return true;
		try {
			if(isCluster(groupName)){
				return getBinaryJedisClusterCommands(groupName).pexpire(key, seconds * 1000) == 1;
			}
			return getBinaryJedisCommands(groupName).pexpire(key, seconds * 1000) == 1;
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
			if(isCluster(groupName)){
				return getBinaryJedisClusterCommands(groupName).pexpireAt(key, expireAt.getTime()) == 1;
			}
			return getBinaryJedisCommands(groupName).pexpireAt(key, expireAt.getTime()) == 1;
		} finally {
			getJedisProvider(groupName).release();
		}
	}
	
	/**
	 * 没设置过期时间则设置
	 * @param seconds
	 * @return
	 */
	public boolean setExpireIfNot(long seconds) {
		Long ttl = getTtl();
		if(ttl == -1){
			return setExpire(seconds);
		}
		return ttl >= 0;
	}

	/**
	 * 返回给定 key 的剩余生存时间
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
			long result = 0;
			if(isCluster(groupName)){
				result = getBinaryJedisClusterCommands(groupName).ttl(key);
			}else{					
				result = getBinaryJedisCommands(groupName).ttl(key);
			}
			return result;
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
			if(isCluster(groupName)){
				return getBinaryJedisClusterCommands(groupName).persist(key) == 1;
			}
			return getBinaryJedisCommands(groupName).persist(key) == 1;
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	/**
	 * 返回 key 所储存的值的类型。
	 * 
	 * @param key
	 * @return none (key不存在)
	 * 
	 *         string (字符串)
	 * 
	 *         list (列表)
	 * 
	 *         set (集合)
	 * 
	 *         zset (有序集)
	 * 
	 *         hash (哈希表)
	 */
	public String type() {
		try {
			if(isCluster(groupName)){
				return getBinaryJedisClusterCommands(groupName).type(key);
			}
			return getBinaryJedisCommands(groupName).type(key);
		} finally {
			getJedisProvider(groupName).release();
		}

	}

	protected byte[] valueSerialize(Object value) {
		try {
			return SerializeUtils.serialize(value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	protected byte[][] valuesSerialize(Object... objects) {
		try {
			byte[][] many = new byte[objects.length][];
			for (int i = 0; i < objects.length; i++) {
				many[i] = SerializeUtils.serialize(objects[i]);
			}
			return many;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T valueDerialize(byte[] bytes) {
		if(bytes == null)return null;
		try {
			return (T)SerializeUtils.deserialize(bytes);
		} catch (Exception e) {
			remove();
			logger.warn("get key[{}] from redis is not null,but Deserialize error,message:{}",origKey,e);
			return null;
		}
	}
	
	protected <T> List<T> listDerialize(List<byte[]> datas){
		List<T> list = new ArrayList<>();
		if(datas == null)return list;
         for (byte[] bs : datas) {
        	 list.add((T)valueDerialize(bs));
		}
		return list;
	}

	/**
	 * 默认过期时间
	 * @return
	 */
	public static long getDefaultExpireSeconds(){
		return CacheExpires.IN_1WEEK + RandomUtils.nextLong(1, CacheExpires.IN_1DAY);
	}
}
