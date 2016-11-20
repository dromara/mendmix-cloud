/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.provider;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.cache.command.RedisObject;
import com.jeesuite.cache.command.RedisSortSet;
import com.jeesuite.mybatis.plugin.cache.CacheProvider;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月2日
 */
public class DefaultCacheProvider implements CacheProvider{

	protected static final Logger logger = LoggerFactory.getLogger(DefaultCacheProvider.class);
	//计算关联key集合权重的基数。2016/10/1的时间戳(精确到小时)
	private long baseScoreInRegionKeysSet = 1475251200;

	@Override
	public <T> T get(String key) {
		return new RedisObject(key).get();
	}


	@Override
	public boolean set(String key, Object value, long expired) {
		return new RedisObject(key).set(value, expired);
	}


	@Override
	public boolean remove(String key) {
		return new RedisObject(key).remove();
	}


	@Override
	public void putGroupKeys(String group, String key,long expireSeconds) {
		long score = calcScoreInRegionKeysSet(expireSeconds);
		RedisSortSet redis = new RedisSortSet(group);
		redis.add(score, key);
		redis.setExpire(expireSeconds);
	}

	@Override
	public void clearGroupKeys(String group) {
		RedisSortSet redis = new RedisSortSet(group);
		List<String> keys = redis.get();
		for (String key : keys) {
			new RedisObject(key).remove();
		}
		redis.remove();
	}
	
	/**
	 * 避免关联key集合越积越多，按插入的先后顺序计算score便于后续定期删除。<br>
	 * Score 即为 实际过期时间的时间戳
	 * @return
	 */
	private long calcScoreInRegionKeysSet(long expireSeconds){
		long currentTime = System.currentTimeMillis()/1000;
		long score = currentTime + expireSeconds - this.baseScoreInRegionKeysSet;
		return score;
	}

	@Override
	public void clearGroupKey(String groupKey, String subKey) {
		new RedisSortSet(groupKey).remove(subKey);
	}


	@Override
	public void clearExpiredGroupKeys(String groupKey) {
		long maxScore = System.currentTimeMillis()/1000 - this.baseScoreInRegionKeysSet;
		new RedisSortSet(groupKey).removeByScore(0, maxScore);
		logger.debug("clearExpiredGroupKeys runing:cacheName:{} , score range:0~{}",groupKey,maxScore);
	}
	
	@Override
	public void close() throws IOException {}

}
