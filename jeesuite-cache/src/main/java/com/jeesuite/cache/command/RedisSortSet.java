/**
 * 
 */
package com.jeesuite.cache.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import redis.clients.util.SafeEncoder;

import static com.jeesuite.cache.redis.JedisProviderFactory.*;

/**
 * redis操作可排序set
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public class RedisSortSet extends RedisCollection {

	public RedisSortSet(String key) {
		super(key);
	}
	
	/**
	 * @param key
	 * @param expireTime 超时时间(秒) 小于等于0 为永久缓存
	 */
	public RedisSortSet(String key,long expireTime) {
		super(key,expireTime);
	}
	
	/**
	 * 指定组名
	 * @param key
	 * @param groupName
	 */
	public RedisSortSet(String key,String groupName) {
		super(key,groupName);
	}
	
	/**
	 * 
	 * @param key
	 * @param groupName 分组名
	 * @param expireTime 超时时间(秒) 小于等于0 为永久缓存
	 */
	public RedisSortSet(String key,String groupName,long expireTime) {
		super(key,groupName,expireTime);
	}
	
	/**
	 * 新增元素
	 * @param score 权重
	 * @param value  元素
	 * @return
	 */
	public boolean add(double score, Object value){
        try {   
        	boolean result = false;
        	if(isCluster(groupName)){
        		result = getBinaryJedisClusterCommands(groupName).zadd(key, score, valueSerialize(value)) >= 1;
        	}else{
        		result = getBinaryJedisCommands(groupName).zadd(key, score, valueSerialize(value)) >= 1;
        	}
        	//设置超时时间
        	if(result)setExpireIfNot(expireTime);
			return result;
    	} finally{
			getJedisProvider(groupName).release();
		}
	}

	/**
	 * 删除有序集合中的一个成员
	 * @param member
	 * @return
	 */
	public boolean remove(Object mem){
        try {   
        	boolean result = false;
        	if(isCluster(groupName)){
        		result = getBinaryJedisClusterCommands(groupName).zrem(key,valueSerialize(mem)) >= 1;
        	}else{
        		result = getBinaryJedisCommands(groupName).zrem(key,valueSerialize(mem)) >= 1;
        	}
			return result;
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
	/**
	 * 查询集合成员数量
	 * @return
	 */
	public long count(){
        try {   
        	long count = 0;
        	if(isCluster(groupName)){
        		count = getBinaryJedisClusterCommands(groupName).zcard(key);
        	}else{
        		count = getBinaryJedisCommands(groupName).zcard(key);
        	}
			return count;
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
	
	
	/**ß
     * 获取全部列表
     * @return
     */
    public <T> List<T> get(){
    	return range(0, -1);
    }
    
    /**
     * 按指定区间取出元素列表
     * @param start
     * @param end
     * @return
     */
    public <T> List<T> range(int start,int end){
    	Set<byte[]> result = null;
        try {    		
        	if(isCluster(groupName)){
        		result = getBinaryJedisClusterCommands(groupName).zrange(key, start, end);
        	}else{
        		result = getBinaryJedisCommands(groupName).zrange(key, start, end);
        	}
        	return toObjectList(new ArrayList<>(result));
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
    
    public boolean removeByScore(long min,long max){
        try {   
        	boolean result = false;
        	byte[] start = SafeEncoder.encode(String.valueOf(min));
        	byte[] end = SafeEncoder.encode(String.valueOf(max));
        	if(isCluster(groupName)){
        		result = getBinaryJedisClusterCommands(groupName).zremrangeByScore(key, start, end) >= 1;
        	}else{
        		result = getBinaryJedisCommands(groupName).zremrangeByScore(key, start, end) >= 1;
        	}
			return result;
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
}
