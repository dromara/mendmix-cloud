/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.provider;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.jeesuite.mybatis.plugin.cache.CacheHandler;

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
	@SuppressWarnings("rawtypes")//
	private RedisSerializer keySerializer;
	@SuppressWarnings("rawtypes")//
	private RedisSerializer valueSerializer;

	public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.keySerializer = redisTemplate.getKeySerializer();
		this.valueSerializer = redisTemplate.getValueSerializer();
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
	public boolean set(String key, Object value, long expired) {
		if(value == null)return false;
		if(value instanceof String){
			stringRedisTemplate.opsForValue().set(key, value.toString(), expired, TimeUnit.SECONDS);
		}else{
			redisTemplate.opsForValue().set(key, value, expired, TimeUnit.SECONDS);
		}
		return true;
	}


	@Override
	public boolean remove(String key) {
		redisTemplate.delete(key);
		return true;
	}


	@Override
	public void putGroupKeys(String cacheGroupKey, String subKey, long expireSeconds) {
		long score = calcScoreInRegionKeysSet(expireSeconds);
		stringRedisTemplate.opsForZSet().add(cacheGroupKey, subKey, score);
	}


	@Override
	public void clearGroupKeys(String cacheGroupKey) {
		
		Set<String> keys = stringRedisTemplate.opsForZSet().range(cacheGroupKey, 0, -1);
		if(keys.isEmpty())return;
		final Object[] keysToArray = keys.toArray(new String[0]);
		
		redisTemplate.execute(new RedisCallback<Void>() {
			@Override
			public Void doInRedis(RedisConnection connection) throws DataAccessException {

				byte[][] keyArray = new byte[keysToArray.length][];
				//先转成key的序列化格式
				for (int i = 0; i < keyArray.length; i++) {
					keyArray[i] = keySerializer.serialize(keysToArray[i]);
				}
				connection.del(keyArray);
				logger.debug("cascade remove cache keyPattern:{},size:{}",cacheGroupKey,keysToArray.length);
			
				return null;
			}
		});
		
		stringRedisTemplate.opsForZSet().remove(cacheGroupKey, keysToArray);
	}


	@Override
	public void clearGroupKey(String cacheGroupKey, String subKey) {
		stringRedisTemplate.opsForZSet().remove(cacheGroupKey, subKey);
		redisTemplate.delete(subKey);
	}


	@Override
	public void clearGroup(String groupName) {
		//清除缓存组的key
		clearGroupKeys(groupName + CacheHandler.GROUPKEY_SUFFIX);
		
		//清除按ID缓存的key
		redisTemplate.execute(new RedisCallback<Void>() {
			@Override
			public Void doInRedis(RedisConnection connection) throws DataAccessException {
				//删除ID的缓存
				String idKeyPattern = groupName + ".id:*";
				Set<byte[]> idKeys = connection.keys(idKeyPattern.getBytes());
				if(idKeys.size() > 0){					
					connection.del(idKeys.toArray(new byte[0][0]));
					logger.debug("cascade remove cache keyPattern:{},size:{}",idKeyPattern,idKeys.size());
				}
				return null;
			}
		});
		
	}
	
	@Override
	public void clearExpiredGroupKeys(String cacheGroup) {
		long maxScore = System.currentTimeMillis()/1000 - this.baseScoreInRegionKeysSet;
		stringRedisTemplate.opsForZSet().removeRangeByScore(cacheGroup, 0, maxScore);
		logger.debug("ClearExpiredRegionKeysTimer runing:cacheName:{} , score range:0~{}",cacheGroup,maxScore);
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		Validate.notNull(stringRedisTemplate," [stringRedisTemplate] is required");
		Validate.notNull(redisTemplate," [redisTemplate] is required");
	}

}
