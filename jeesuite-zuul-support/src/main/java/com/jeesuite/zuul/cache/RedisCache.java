package com.jeesuite.zuul.cache;

import com.jeesuite.cache.command.RedisBatchCommand;
import com.jeesuite.cache.command.RedisHashMap;
import com.jeesuite.cache.command.RedisObject;
import com.jeesuite.cache.command.RedisString;
import com.jeesuite.zuul.Cache;

/**
 * 
 * <br>
 * Class Name   : RedisCache
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年7月10日
 */
public class RedisCache implements Cache {

	private String groupName;
	private String keyPrefix;
	private long timeToLiveSeconds;
	
	
	public RedisCache(String groupName,String keyPrefix,int timeToLiveSeconds) {
		this.groupName = groupName;
		this.keyPrefix = keyPrefix + ":";
		this.timeToLiveSeconds = timeToLiveSeconds;
	}
	
	private String buildKey(String key){
		return this.keyPrefix.concat(key);
	}

	@Override
	public void setString(String key, String value) {
		new RedisString(buildKey(key),groupName).set(value,timeToLiveSeconds);
	}

	@Override
	public String getString(String key) {
		return new RedisString(buildKey(key),groupName).get();
	}

	@Override
	public void setObject(String key, Object value) {
		new RedisObject(buildKey(key),groupName).set(value,timeToLiveSeconds);
	}

	@Override
	public <T> T getObject(String key) {
		return new RedisObject(buildKey(key),groupName).get();
	}

	@Override
	public void remove(String key) {
		new RedisObject(buildKey(key),groupName).remove();
	}

	@Override
	public void removeAll() {
		RedisBatchCommand.removeByKeyPrefix(groupName,keyPrefix);
	}

	@Override
	public boolean exists(String key) {
		return new RedisObject(buildKey(key),groupName).exists();
	}

	@Override
	public void setMapValue(String key,String field,Object value) {
		new RedisHashMap(key,groupName,timeToLiveSeconds).set(field, value);
	}

	@Override
	public <T> T getMapValue(String key, String field) {
		return new RedisHashMap(key,groupName).getOne(field);
	}


	@Override
	public void updateExpireTime(String key) {
		new RedisObject(buildKey(key),groupName).setExpire(timeToLiveSeconds);
	}

}

