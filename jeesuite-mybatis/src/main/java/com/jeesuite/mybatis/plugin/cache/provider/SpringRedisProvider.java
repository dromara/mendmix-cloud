/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.provider;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.jeesuite.mybatis.plugin.cache.CacheHandler;

/**
 * 自动缓存Spring cache缓存实现
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月13日
 */
@SuppressWarnings("unchecked")
public class SpringRedisProvider extends AbstractCacheProvider {

	private RedisTemplate<String, Object> redisTemplate;
	@SuppressWarnings("rawtypes")//
	private RedisSerializer keySerializer;
	@SuppressWarnings("rawtypes")//
	private RedisSerializer valueSerializer;

	public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.keySerializer = redisTemplate.getKeySerializer();
		this.valueSerializer = redisTemplate.getValueSerializer();
	}

	@Override
	public void close() throws IOException {}

	@Override
	public <T> T get(String key) {
		return (T) redisTemplate.opsForValue().get(key);
	}

	@Override
	public String getStr(String key) {
		return get(key);
	}


	@Override
	public boolean set(String key, Object value, long expired) {
		redisTemplate.opsForValue().set(key, value, expired, TimeUnit.SECONDS);
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
		redisTemplate.opsForZSet().add(cacheGroupKey, subKey, score);
	}


	@Override
	public void clearGroupKeys(String cacheGroupKey) {
		redisTemplate.execute(new RedisCallback<Void>() {
			@Override
			public Void doInRedis(RedisConnection connection) throws DataAccessException {
				byte[] keyBytes = keySerializer.serialize(cacheGroupKey);
				//删除关联组的缓存
				Set<byte[]> keysInGroup = connection.zRange(keyBytes, 0, -1);
				if(keysInGroup != null && keysInGroup.size() > 0){
					byte[][] keyArray = keysInGroup.toArray(new byte[0][0]);
					connection.zRem(keyBytes, keyArray);
					//先转成key的序列化格式
					for (int i = 0; i < keyArray.length; i++) {
						keyArray[i] = keySerializer.serialize(valueSerializer.deserialize(keyArray[i]));
					}
					connection.del(keyArray);
					logger.debug("cascade remove cache keyPattern:{},size:{}",cacheGroupKey,keysInGroup.size());
				}
				return null;
			}
		});
	}


	@Override
	public void clearGroupKey(String cacheGroupKey, String subKey) {
		redisTemplate.opsForZSet().remove(cacheGroupKey, subKey);
		redisTemplate.delete(subKey);
	}


	@Override
	public void clearGroup(String groupName) {
		redisTemplate.execute(new RedisCallback<Void>() {
			@Override
			public Void doInRedis(RedisConnection connection) throws DataAccessException {
				byte[] keyBytes = keySerializer.serialize(groupName + CacheHandler.GROUPKEY_SUFFIX);
				//删除关联组的缓存
				Set<byte[]> keysInRegion = connection.zRange(keyBytes, 0, -1);
				if(keysInRegion.size() > 0){
					byte[][] keyArray = keysInRegion.toArray(new byte[0][0]);
					connection.zRem(keyBytes, keyArray);
					//先转成key的序列化格式
					for (int i = 0; i < keyArray.length; i++) {
						keyArray[i] = keySerializer.serialize(valueSerializer.deserialize(keyArray[i]));
					}
					connection.del(keyArray);
					logger.debug("cascade remove cache keyPattern:{},size:{}",groupName,keysInRegion.size());
				}
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
		redisTemplate.opsForZSet().removeRangeByScore(cacheGroup, 0, maxScore);
		logger.debug("ClearExpiredRegionKeysTimer runing:cacheName:{} , score range:0~{}",cacheGroup,maxScore);
	}

}
