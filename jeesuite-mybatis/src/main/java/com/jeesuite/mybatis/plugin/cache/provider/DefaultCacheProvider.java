/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.provider;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.cache.command.RedisBatchCommand;
import com.jeesuite.cache.command.RedisObject;
import com.jeesuite.cache.command.RedisString;
import com.jeesuite.cache.redis.JedisProviderFactory;
import com.jeesuite.mybatis.plugin.cache.CacheHandler;

import redis.clients.jedis.JedisCommands;

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
	public void putGroup(String cacheGroupKey, String key,long expireSeconds) {
		long score = calcScoreInRegionKeysSet(expireSeconds);
		JedisCommands commands = JedisProviderFactory.getJedisCommands(null);
		try {			
			commands.zadd(cacheGroupKey, score, key);
			commands.pexpire(cacheGroupKey, expireSeconds * 1000);
		} finally{
			JedisProviderFactory.getJedisProvider(null).release();
		}
	}


	@Override
	public void removeFromGroup(String cacheGroupKey, String key) {
		JedisCommands commands = JedisProviderFactory.getJedisCommands(null);
		try {			
			commands.zrem(cacheGroupKey, key);
			//
			commands.del(key);
		} finally{
			JedisProviderFactory.getJedisProvider(null).release();
		}
	}
	
	
	@Override
	public void clearGroup(final String groupName,final boolean containPkCache) {

		String cacheGroupKey = groupName + CacheHandler.GROUPKEY_SUFFIX;
		JedisCommands commands = JedisProviderFactory.getJedisCommands(null);
		try {	
			Set<String> keys = commands.zrange(cacheGroupKey, 0, -1);
			//删除实际的缓存
			if(keys != null && keys.size() > 0){
				RedisBatchCommand.removeObjects(keys.toArray(new String[0]));
			}
			commands.del(cacheGroupKey);
			//删除按ID缓存的
			if(containPkCache){				
				keys = JedisProviderFactory.getMultiKeyCommands(null).keys(groupName +".id:*");
				if(keys != null && keys.size() > 0){
					RedisBatchCommand.removeObjects(keys.toArray(new String[0]));
				}
			}
			
		} finally{
			JedisProviderFactory.getJedisProvider(null).release();
		}
	
	}
	
	@Override
	public void clearExpiredGroupKeys(String cacheGroup) {
		long maxScore = System.currentTimeMillis() / 1000 - this.baseScoreInRegionKeysSet;
		JedisCommands commands = JedisProviderFactory.getJedisCommands(null);
		try {
			commands.zremrangeByScore(cacheGroup, 0, maxScore);
		} finally {
			JedisProviderFactory.getJedisProvider(null).release();
		}
		logger.debug("clearExpiredGroupKeys runing:cacheName:{} , score range:0~{}", cacheGroup, maxScore);
	}
	
	@Override
	public boolean exists(String key) {
		return new RedisObject(key).exists();
	}

	
	@Override
	public void close() throws IOException {}

	

}
