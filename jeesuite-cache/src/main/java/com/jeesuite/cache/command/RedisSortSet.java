/**
 * 
 */
package com.jeesuite.cache.command;

import static com.jeesuite.cache.redis.JedisProviderFactory.getBinaryJedisClusterCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getBinaryJedisCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisProvider;
import static com.jeesuite.cache.redis.JedisProviderFactory.isCluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * redis操作可排序set
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public class RedisSortSet extends RedisBinaryCollection {

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
	public long add(double score, Object value){
        try {   
        	long result = 0;
        	if(isCluster(groupName)){
        		result = getBinaryJedisClusterCommands(groupName).zadd(keyBytes, score, valueSerialize(value));
        	}else{
        		result = getBinaryJedisCommands(groupName).zadd(keyBytes, score, valueSerialize(value));
        	}
        	//设置超时时间
        	if(result > 0)setExpireIfNot(expireTime);
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
        		result = getBinaryJedisClusterCommands(groupName).zrem(keyBytes,valueSerialize(mem)) >= 1;
        	}else{
        		result = getBinaryJedisCommands(groupName).zrem(keyBytes,valueSerialize(mem)) >= 1;
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
        		count = getBinaryJedisClusterCommands(groupName).zcard(keyBytes);
        	}else{
        		count = getBinaryJedisCommands(groupName).zcard(keyBytes);
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
    public <T> List<T> range(long start,long end){
    	Set<byte[]> result = null;
        try {    		
        	if(isCluster(groupName)){
        		result = getBinaryJedisClusterCommands(groupName).zrange(keyBytes, start, end);
        	}else{
        		result = getBinaryJedisCommands(groupName).zrange(keyBytes, start, end);
        	}
        	return toObjectList(new ArrayList<>(result));
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
    
    /**
     * 按指定权重取出元素列表
     * @param start
     * @param end
     * @return
     */
    public <T> List<T> getByScore(double min,double max){
    	Set<byte[]> result = null;
        try {    		
        	if(isCluster(groupName)){
        		result = getBinaryJedisClusterCommands(groupName).zrangeByScore(keyBytes, min, max);
        	}else{
        		result = getBinaryJedisCommands(groupName).zrangeByScore(keyBytes, min, max);
        	}
        	return toObjectList(new ArrayList<>(result));
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
    
    /**
     * 按权重删除
     * @param min
     * @param max
     * @return
     */
    public long removeByScore(double min,double max){
        try {   
        	long result = 0;
        	if(isCluster(groupName)){
        		result = getBinaryJedisClusterCommands(groupName).zremrangeByScore(keyBytes, min, max);
        	}else{
        		result = getBinaryJedisCommands(groupName).zremrangeByScore(keyBytes, min, max);
        	}
			return result;
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
    
    public double getScore(String value){
        try {   
        	Double result;
        	if(isCluster(groupName)){
        		result = getBinaryJedisClusterCommands(groupName).zscore(keyBytes, valueSerialize(value));
        	}else{
        		result = getBinaryJedisCommands(groupName).zscore(keyBytes, valueSerialize(value)); 
        	}
			return result == null ?  -1 : result;
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
}
