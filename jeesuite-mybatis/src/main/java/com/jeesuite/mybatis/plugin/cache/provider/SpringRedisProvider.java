/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.provider;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 自动缓存Spring cache缓存实现
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月13日
 */
@SuppressWarnings("unchecked")
public class SpringRedisProvider extends AbstractCacheProvider implements InitializingBean {

	private RedisTemplate<String, Object> redisTemplate;
	private StringRedisTemplate stringRedisTemplate;

	public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}



	@Override
	public void close() throws IOException {}

	@Override
	public <T> T get(String key) {
		return (T) redisTemplate.opsForValue().get(key);
	}

	@Override
	public String getStr(String key) {
		return stringRedisTemplate.opsForValue().get(key);
	}


	@Override
	public boolean set(String key, Object value, long expireSeconds) {
		if(value == null)return false;
		redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
		return true;
	}
	
	@Override
	public boolean setStr(String key, Object value, long expireSeconds) {
		if(value == null)return false;
		stringRedisTemplate.opsForValue().set(key, value.toString(), expireSeconds, TimeUnit.SECONDS);
		return true;
	}
	
	@Override
	public boolean remove(String key) {
		redisTemplate.delete(key);
		return true;
	}

	
	@Override
	public boolean exists(String key) {
		return redisTemplate.hasKey(key);
	}
	
	@Override
	public void addZsetValue(String key, String value, double score) {
		
	}


	@Override
	public boolean existZsetValue(String key, String value) {
		return false;
	}
	
	@Override
	public boolean removeZsetValue(String key, String value) {
		return false;
	}

	@Override
	public boolean removeZsetValues(String key, double minScore, double maxScore) {
		return false;
	}

	@Override
	public boolean setnx(String key, String value, long expireSeconds) {
		return false;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Validate.notNull(stringRedisTemplate," [stringRedisTemplate] is required");
		Validate.notNull(redisTemplate," [redisTemplate] is required");
	}
	
	

}
