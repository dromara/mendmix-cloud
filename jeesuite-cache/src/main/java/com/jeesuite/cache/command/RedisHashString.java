/**
 * 
 */
package com.jeesuite.cache.command;

import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisClusterCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisProvider;
import static com.jeesuite.cache.redis.JedisProviderFactory.isCluster;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * redis操作hashmap
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public class RedisHashString {
	/**
	 *  默认缓存时长（7 天）
	 */
	protected static final int DEFAULT_EXPIRE_TIME = 60 * 60 * 24 * 7;

	protected static final String RESP_OK = "OK";

	protected String key;

	protected String groupName;

	public RedisHashString(String key) {
		this.key = key;
		if (key.contains(RedisBase.KEY_SUFFIX_SPLIT)) {
			this.groupName = key.split(RedisBase.KEY_SUFFIX_SPLIT)[0];
		}
	}

	/**
	 * 
	 * @param key
	 * @param groupName
	 *            组名
	 */
	public RedisHashString(String key, String groupName) {
		this.key = key;
		this.groupName = groupName;
	}

	/**
	 * 检查给定 key 是否存在。
	 *
	 * @return
	 */
	public boolean exists() {
		try {
			if(isCluster(groupName)){
				return getJedisClusterCommands(groupName).exists(key);
			}
			return getJedisClusterCommands(groupName).exists(key);
		} finally {
			getJedisProvider(groupName).release();
		}

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
			if (isCluster(groupName)) {
				result = getJedisClusterCommands(groupName).hmset(key, datas).equals(RESP_OK);
			} else {
				result = getJedisCommands(groupName).hmset(key, datas).equals(RESP_OK);
			}
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
			Map<String, String> result = new HashMap<>();
			if (isCluster(groupName)) {
				result = getJedisClusterCommands(groupName).hgetAll(key);
			} else {
				result = getJedisCommands(groupName).hgetAll(key);
			}
			return result;
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
			if (isCluster(groupName)) {
				return getJedisClusterCommands(groupName).hexists(key, field);
			} else {
				return getJedisCommands(groupName).hexists(key, field);
			}
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
	public boolean set(String field, Object value) {
		boolean result = false;
		if(value == null)return false;
		//返回值（1:新字段被设置,0:已经存在值被更新）
		try {
			if (isCluster(groupName)) {
				result = getJedisClusterCommands(groupName)
						.hset(key, field, value.toString()) >= 0;
			} else {
				result = getJedisCommands(groupName).hset(key, field, value.toString()) >= 0;
			}		
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
			if (isCluster(groupName)) {
				return getJedisClusterCommands(groupName).hdel(key, field).equals(RESP_OK);
			} else {
				return getJedisCommands(groupName).hdel(key, field).equals(RESP_OK);
			}
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
			if (isCluster(groupName)) {
				return getJedisClusterCommands(groupName).hlen(key);
			} else {
				return getJedisCommands(groupName).hlen(key);
			}
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
			List<String> datas = null;
			Map<String, String> result = new HashMap<>();
			for (String field : fields) {
				if (isCluster(groupName)) {
					datas = getJedisClusterCommands(groupName).hmget(key, field);
				} else {
					datas = getJedisCommands(groupName).hmget(key, field);
				}
				result.put(field, datas.get(0));
			}
			return result;
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
		try {
			if(isCluster(groupName)){
				return getJedisClusterCommands(groupName).pexpire(key, seconds * 1000) == 1;
			}
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
			if(isCluster(groupName)){
				return getJedisClusterCommands(groupName).pexpireAt(key, expireAt.getTime()) == 1;
			}
			return getJedisCommands(groupName).pexpireAt(key, expireAt.getTime()) == 1;
		} finally {
			getJedisProvider(groupName).release();
		}
	}
	
	public Long getTtl() {
		try {
			long result = 0;
			if(isCluster(groupName)){
				result = getJedisClusterCommands(groupName).ttl(key);
			}else{					
				result = getJedisCommands(groupName).ttl(key);
			}
			return result;
		} finally {
			getJedisProvider(groupName).release();
		}
		
	}

	
	/**
	 * 根据指定字段将其值进行自增(递增值为1)
	 *
	 * @param fields
     */
	public void increment(String... fields) {
		if (fields != null && fields.length > 0) {
			Map<String, Long> expects = new HashMap<>(fields.length);
			for (String field : fields) {
				expects.put(field, 1L);
			}
			this.increment(expects);
		}
	}

	/**
	 * 根据指定字段及递增值进行自增
	 *
	 * @param field
	 * @param value
     */
	public void increment(String field, long value) {
		Map<String, Long> expects = new HashMap<>(1);
		expects.put(field, value);
		this.increment(expects);
	}

	/**
	 * 根据指定字段及递增值进行自增
	 *
	 * @param expects
     */
	public void increment(Map<String, Long> expects) {
		try {
			expects.forEach((k, v) -> {
				if (isCluster(groupName)) {
					getJedisClusterCommands(groupName).hincrBy(key, k, v);
				} else {
					getJedisCommands(groupName).hincrBy(key, k, v);
				}
			});
		} finally {
			getJedisProvider(groupName).release();
		}
	}

}
