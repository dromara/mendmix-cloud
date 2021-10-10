package com.jeesuite.mybatis.plugin.cache.provider;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.jeesuite.cache.command.RedisBase;
import com.jeesuite.mybatis.MybatisConfigs;
import com.jeesuite.mybatis.plugin.cache.CacheHandler;
import com.jeesuite.spring.InstanceFactory;

public class SpringRedisProvider extends AbstractCacheProvider  {

	private RedisTemplate<String, Object> redisTemplate;
	private StringRedisTemplate stringRedisTemplate;


	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SpringRedisProvider(String groupName) {
		super(groupName);
		Map<String, RedisOperations> instanceMap = InstanceFactory.getInstanceProvider().getInterfaces(RedisOperations.class);
		Collection<RedisOperations> instances = instanceMap.values();
		for (RedisOperations redis : instances) {
			if(redis instanceof StringRedisTemplate) {
				stringRedisTemplate = (StringRedisTemplate) redis;
			}else {
				redisTemplate = (RedisTemplate<String, Object>) redis;
			}
		}
	}

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
		redisTemplate.opsForValue().setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS);
		return true;
	}

	@Override
	public boolean setStr(String key, Object value, long expireSeconds) {
		if(value == null)return false;
		stringRedisTemplate.opsForValue().set(key, value.toString(), expireSeconds, TimeUnit.SECONDS);
		return true;
	}

	@Override
	public boolean remove(String...keys) {
		if(keys.length == 1) {
			return redisTemplate.delete(keys[0]);
		}
		return redisTemplate.delete(Arrays.asList(keys)) > 0;
	}

	@Override
	public boolean exists(String key) {
		return redisTemplate.hasKey(key);
	}
	
	@Override
	public void setExpire(String key, long expireSeconds) {
		redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
	}


	@Override
	public void putGroup(String cacheGroupKey, String key) {
		if(MybatisConfigs.isSchameSharddingTenant(groupName)) {
			key = RedisBase.buildTenantNameSpaceKey(key);
		}
		stringRedisTemplate.opsForList().leftPush(cacheGroupKey, key);
	}

	@Override
	public void clearGroup(String groupName, String... prefixs) {
		String cacheGroupKey = groupName.endsWith(CacheHandler.GROUPKEY_SUFFIX) ? groupName : groupName + CacheHandler.GROUPKEY_SUFFIX;
		
		stringRedisTemplate.opsForList().size(cacheGroupKey);
	}

	@Override
	public boolean setnx(String key, String value, long expireSeconds) {
		return stringRedisTemplate.opsForValue().setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS);
	}
	
	@Override
	public List<String> getListItems(String key, int start, int end) {
		return stringRedisTemplate.opsForList().range(key, start, end);
	}
	
	@Override
	public long getListSize(String key) {
		return stringRedisTemplate.opsForList().size(key);
	}

	@Override
	public void close() throws IOException {}
		
}
