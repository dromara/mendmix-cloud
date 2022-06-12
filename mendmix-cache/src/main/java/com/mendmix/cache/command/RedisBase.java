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
package com.mendmix.cache.command;

import static com.mendmix.cache.redis.JedisProviderFactory.getBinaryJedisClusterCommands;
import static com.mendmix.cache.redis.JedisProviderFactory.getBinaryJedisCommands;
import static com.mendmix.cache.redis.JedisProviderFactory.getJedisCommands;
import static com.mendmix.cache.redis.JedisProviderFactory.getJedisProvider;
import static com.mendmix.cache.redis.JedisProviderFactory.isCluster;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.cache.CacheExpires;
import com.mendmix.cache.redis.JedisProviderFactory;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.util.SerializeUtils;

import redis.clients.jedis.util.SafeEncoder;

/**
 * redis基础操作指令
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public abstract class RedisBase {
	
	protected static final Logger logger = LoggerFactory.getLogger(RedisBase.class);
	
	public static final String TENANT_KEY_PREFIX = "_tenant@";
	private static final String TENANT_ID_KEY = "_ctx_tenantId_";
	private static final String KEY_SPLITER = ":";
	
	protected static final String RESP_OK = "OK";
	//
	//
    protected String groupName;
    
	protected byte[] keyBytes;
	
	protected String key;
	
	boolean isBinary = true;
	
	public String key() {
		return key;
	}
	
	public byte[] getKey() {
		return keyBytes;
	}

	public RedisBase(String key) {
		this(key, null,true);
	}
	
	public RedisBase(String key,boolean isBinary) {
		this(key, null, isBinary);
	}
	
	public RedisBase(String key,String groupName,boolean isBinary) {
		this.groupName = groupName;
		if(getJedisProvider(groupName).tenantMode()){
			this.key = buildTenantNameSpaceKey(key);
		}else{
			this.key = key;
		}
		this.isBinary = isBinary;
		if(isBinary)this.keyBytes = SafeEncoder.encode(this.key);
	}
	
	public static String buildTenantNameSpaceKey(String key){
		String tenantId = ThreadLocalContext.getStringValue(TENANT_ID_KEY);
		if(tenantId == null)throw new NullPointerException("无法识别租户");
		if(key.startsWith(TENANT_KEY_PREFIX))return key;
		return new StringBuilder(TENANT_KEY_PREFIX).append(tenantId).append(KEY_SPLITER).append(key).toString();
	}

	/**
	 * 检查给定 key 是否存在。
	 * 
	 * @param keyBytes
	 * @return
	 */
	public boolean exists() {
		try {
			if(!isBinary)return getJedisCommands(groupName).exists(key);
			if(isCluster(groupName)){
				return getBinaryJedisClusterCommands(groupName).exists(keyBytes);
			}
			return getBinaryJedisCommands(groupName).exists(keyBytes);
		} finally {
			getJedisProvider(groupName).release();
		}
		
	}
	

	/**
	 * 删除给定的一个 key 。
	 * 
	 * 不存在的 key 会被忽略。
	 * 
	 * @param keyBytes
	 * @return true：存在该key删除时返回
	 * 
	 *         false：不存在该key
	 */
	public boolean remove() {
		try {
			if(!isBinary)return getJedisCommands(groupName).del(key) == 1;
			if(isCluster(groupName)){
				return getBinaryJedisClusterCommands(groupName).del(keyBytes) == 1;
			}
			return getBinaryJedisCommands(groupName).del(keyBytes) == 1;
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	/**
	 * 为给定 key 设置生存时间，当 key 过期时(生存时间为 0 )，它会被自动删除。
	 * 
	 * @param keyBytes
	 * @param seconds
	 *            超时时间，单位：秒
	 * @return true：超时设置成功
	 * 
	 *         false：key不存在或超时未设置成功
	 */
	public boolean setExpire(long seconds) {
		if(seconds <= 0)return true;
		try {
			if(!isBinary)return getJedisCommands(groupName).expire(key, (int)seconds) == 1;
			if(isCluster(groupName)){
				return getBinaryJedisClusterCommands(groupName).expire(keyBytes, (int)seconds) == 1;
			}
			return getBinaryJedisCommands(groupName).expire(keyBytes, (int)seconds) == 1;
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
	 * @param keyBytes
	 * @param expireAt
	 *            超时时间点
	 * @return true：超时设置成功
	 *
	 *         false：key不存在或超时未设置成功
	 */
	public boolean setExpireAt(Date expireAt) {
		try {
			if(!isBinary)return getJedisCommands(groupName).pexpireAt(key, expireAt.getTime()) == 1;
			if(isCluster(groupName)){
				return getBinaryJedisClusterCommands(groupName).pexpireAt(keyBytes, expireAt.getTime()) == 1;
			}
			return getBinaryJedisCommands(groupName).pexpireAt(keyBytes, expireAt.getTime()) == 1;
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
		if(seconds <= 0)return true;
		Long ttl = getTtl();
		if(ttl == -1){
			return setExpire(seconds);
		}
		return ttl >= 0;
	}

	/**
	 * 返回给定 key 的剩余生存时间(单位：秒)
	 * 
	 * @param keyBytes
	 * @return 当 key 不存在时，返回 -2 。
	 *         当 key 存在但没有设置剩余生存时间时，返回 -1 。
	 *         否则返回 key的剩余生存时间。
	 */
	public Long getTtl() {
		try {
			if(!isBinary)return getJedisCommands(groupName).ttl(key);
			long result = 0;
			if(isCluster(groupName)){
				result = getBinaryJedisClusterCommands(groupName).ttl(keyBytes);
			}else{					
				result = getBinaryJedisCommands(groupName).ttl(keyBytes);
			}
			return result;
		} finally {
			getJedisProvider(groupName).release();
		}
		
	}

	/**
	 * 移除给定 key 的生存时间，设置为永久有效
	 * 
	 * @param keyBytes
	 * @return 当生存时间移除成功时，返回 1 .
	 * 
	 *         如果 key 不存在或 key 没有设置生存时间，返回 0 。
	 */
	public boolean removeExpire() {
		try {
			if(!isBinary)return getJedisCommands(groupName).persist(key) == 1;
			if(isCluster(groupName)){
				return getBinaryJedisClusterCommands(groupName).persist(keyBytes) == 1;
			}
			return getBinaryJedisCommands(groupName).persist(keyBytes) == 1;
		} finally {
			getJedisProvider(groupName).release();
		}
	}

	/**
	 * 返回 key 所储存的值的类型。
	 * 
	 * @param keyBytes
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
				return getBinaryJedisClusterCommands(groupName).type(keyBytes);
			}
			return getBinaryJedisCommands(groupName).type(keyBytes);
		} finally {
			getJedisProvider(groupName).release();
		}

	}
	
	/**
	 * 查找所有符合给定模式 pattern 的 key
	 * @param pattern
	 * @return
	 */
	public Set<String> keys(String pattern){
		Set<String> keys = JedisProviderFactory.getMultiKeyCommands(groupName).keys(pattern);
		return keys;
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
			logger.warn("MENDMIX-TRACE-LOGGGING-->> get key[{}] from redis is not null,but Deserialize error,message:{}",key,e);
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
		return CacheExpires.todayEndSeconds();
	}
}
