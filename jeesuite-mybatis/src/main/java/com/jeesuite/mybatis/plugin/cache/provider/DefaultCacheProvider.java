/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.provider;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.cache.command.RedisObject;
import com.jeesuite.cache.command.RedisSortSet;
import com.jeesuite.cache.command.RedisString;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月2日
 */
public class DefaultCacheProvider extends AbstractCacheProvider{

	protected static final Logger logger = LoggerFactory.getLogger(DefaultCacheProvider.class);
	
	@Override
	public <T> T get(String key) {
		return new RedisObject(key).get();
	}

	@Override
	public String getStr(String key){
		return new RedisString(key).get();
	}

	@Override
	public boolean set(String key, Object value, long expireSeconds) {
		if(value == null)return false;
		return new RedisObject(key).set(value, expireSeconds);
	}
	
	@Override
	public boolean setStr(String key, Object value, long expireSeconds) {
		if(value == null)return false;
		return new RedisString(key).set(value.toString(),expireSeconds);
	}


	@Override
	public boolean remove(String key) {
		return new RedisObject(key).remove();
	}

	@Override
	public boolean exists(String key) {
		return new RedisObject(key).exists();
	}

	@Override
	public void addZsetValue(String key, String value, double score) {
		new RedisSortSet(key).add(score, value);
	}


	@Override
	public boolean existZsetValue(String key, String value) {
		return new RedisSortSet(key).getScore(value) > 0;
	}
	
	@Override
	public boolean removeZsetValue(String key, String value) {
		return new RedisSortSet(key).remove(value);
	}

	@Override
	public boolean removeZsetValues(String key, double minScore, double maxScore) {
		return new RedisSortSet(key).removeByScore(minScore, maxScore) > 0;
	}

	@Override
	public boolean setnx(String key, String value, long expireSeconds) {
		return new RedisString(key).setnx(value, expireSeconds);
	}
	
	@Override
	public void close() throws IOException {}

}
