/**
 * 
 */
package com.jeesuite.cache.command;

import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisProvider;

import java.util.Set;

/**
 * redis操作可排序set
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public class RedisStrSet extends RedisBase {

private long expireTime;//过期时间（秒）
	
	public RedisStrSet(String key) {
		this(key, null, RedisBase.getDefaultExpireSeconds());
	}
	/**
	 * 
	 * @param key
	 * @param groupName 组名
	 */
	public RedisStrSet(String key,String groupName) {
		this(key, groupName, RedisBase.getDefaultExpireSeconds());
	}
	
	public RedisStrSet(String key,String groupName,long expireTime) {
		super(key,groupName,false);
		this.expireTime = expireTime;
	}
	
	public long add(String... strs) {
        try {   
        	long result = getJedisCommands(groupName).sadd(key,strs);
        	//设置超时时间
        	if(result > 0)setExpireIfNot(expireTime);
			return result;
    	} finally{
			getJedisProvider(groupName).release();
		}
	}

	public Set<String> get() {
        try {    		
        	return getJedisCommands(groupName).smembers(key);
    	} finally{
			getJedisProvider(groupName).release();
		}
	}

	
	public boolean remove(String... strs) {
        try {
        	return getJedisCommands(groupName).srem(key,strs) == 1;		
    	} finally{
			getJedisProvider(groupName).release();
		}
	}

	public long length() {
        try {    		
        	return getJedisCommands(groupName).scard(key);
    	} finally{
			getJedisProvider(groupName).release();
		}
	}

	public boolean contains(String value) {
        try {    		
        	return getJedisCommands(groupName).sismember(key, value);
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
	
	public boolean containsAny(String...objects) {
        try {  
        	for (String object : objects) {
        		if(getJedisCommands(groupName).sismember(key, object))return true;
			}
        	return false;
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
}
