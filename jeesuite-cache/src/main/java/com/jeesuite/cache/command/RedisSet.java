/**
 * 
 */
package com.jeesuite.cache.command;

import static com.jeesuite.cache.redis.JedisProviderFactory.getBinaryJedisClusterCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getBinaryJedisCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisProvider;
import static com.jeesuite.cache.redis.JedisProviderFactory.isCluster;

import java.util.HashSet;
import java.util.Set;

/**
 * 对象redis操作set
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public class RedisSet extends RedisBinaryCollection {

	public RedisSet(String key) {
		super(key);
	}
	
	/**
	 * @param key
	 * @param expireTime 超时时间(秒) 小于等于0 为永久缓存
	 */
	public RedisSet(String key,long expireTime) {
		super(key,expireTime);
	}
	
	/**
	 * 指定组名
	 * @param key
	 * @param groupName
	 */
	public RedisSet(String key,String groupName) {
		super(key,groupName);
	}
	
	/**
	 * 
	 * @param key
	 * @param groupName 分组名
	 * @param expireTime 超时时间(秒) 小于等于0 为永久缓存
	 */
	public RedisSet(String key,String groupName,long expireTime) {
		super(key,groupName,expireTime);
	}
	
	public long add(Object... objects) {
        try {   
        	long result = 0;
        	byte[][] datas = valuesSerialize(objects);
        	if(isCluster(groupName)){
        		result = getBinaryJedisClusterCommands(groupName).sadd(keyBytes,datas);
        	}else{
        		result = getBinaryJedisCommands(groupName).sadd(keyBytes,datas);
        	}
        	//设置超时时间
        	if(result > 0)setExpireIfNot(expireTime);
			return result;
    	} finally{
			getJedisProvider(groupName).release();
		}
	}

	public <T> Set<T> get() {
		Set<byte[]> datas = null;
        try {    		
        	if(isCluster(groupName)){
        		datas = getBinaryJedisClusterCommands(groupName).smembers(keyBytes);
        	}else{
        		datas = getBinaryJedisCommands(groupName).smembers(keyBytes);
        	}
        	return toObjectSet(datas);
    	} finally{
			getJedisProvider(groupName).release();
		}
	}

	
	public boolean remove(Object... objects) {
        try {
        	byte[][] datas = valuesSerialize(objects);
        	if(isCluster(groupName)){
        		return getBinaryJedisClusterCommands(groupName).srem(keyBytes,datas) == 1;
        	}else{
        		return getBinaryJedisCommands(groupName).srem(keyBytes,datas) == 1;
        	}		
    	} finally{
			getJedisProvider(groupName).release();
		}
	}

	public long length() {
        try {    		
        	if(isCluster(groupName)){
        		return getBinaryJedisClusterCommands(groupName).scard(keyBytes);
        	}else{
        		return getBinaryJedisCommands(groupName).scard(keyBytes);
        	}
    	} finally{
			getJedisProvider(groupName).release();
		}
	}

	public boolean contains(Object object) {
        try {    		
        	if(isCluster(groupName)){
        		return getBinaryJedisClusterCommands(groupName).sismember(keyBytes, valueSerialize(object));
        	}else{
        		return getBinaryJedisCommands(groupName).sismember(keyBytes, valueSerialize(object));
        	}
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
	

	protected <T> Set<T> toObjectSet(Set<byte[]> datas) {
		Set<T> result = new HashSet<>();
    	if(datas == null)return result;
    	for (byte[] data : datas) {
			result.add((T)valueDerialize(data));
		}
		return result;
	}
}
