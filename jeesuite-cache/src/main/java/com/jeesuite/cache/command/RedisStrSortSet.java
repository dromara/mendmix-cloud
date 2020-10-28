/**
 * 
 */
package com.jeesuite.cache.command;

import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * redis操作可排序set
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public class RedisStrSortSet extends RedisBase {

private long expireTime;//过期时间（秒）
	
	public RedisStrSortSet(String key) {
		this(key, null, RedisBase.getDefaultExpireSeconds());
	}
	/**
	 * 
	 * @param key
	 * @param groupName 组名
	 */
	public RedisStrSortSet(String key,String groupName) {
		this(key, groupName, RedisBase.getDefaultExpireSeconds());
	}
	
	public RedisStrSortSet(String key,String groupName,long expireTime) {
		super(key,groupName,false);
		this.expireTime = expireTime;
	}
	
	/**
	 * 新增元素
	 * @param score 权重
	 * @param value  元素
	 * @return
	 */
	public boolean add(double score, String value){
        try {   
        	boolean result = getJedisCommands(groupName).zadd(key, score, value) >= 1;
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
	public boolean remove(String mem){
        try {   
			return getJedisCommands(groupName).zrem(key,mem) >= 1;
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
        	long count = getJedisCommands(groupName).zcard(key);
			return count;
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
	
	
	/**ß
     * 获取全部列表
     * @return
     */
    public List<String> get(){
    	return range(0, -1);
    }
    
    /**
     * 按指定区间取出元素列表
     * @param start
     * @param end
     * @return
     */
    public List<String> range(long start,long end){
        try {    		
        	return new ArrayList<>(getJedisCommands(groupName).zrange(key, start, end));
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
    
    public List<String> rangeByScore(double min,double max){
        try {    		
        	return new ArrayList<>(getJedisCommands(groupName).zrangeByScore(key, min, max));
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
    
    public long removeByScore(double min,double max){
        try {   
			return getJedisCommands(groupName).zremrangeByScore(key, min, max);
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
    
    public double getScore(String member){
        try {   
			Double zscore = getJedisCommands(groupName).zscore(key, member);
			return zscore == null ? -1 : zscore.doubleValue();
    	} finally{
			getJedisProvider(groupName).release();
		}
	}
}
