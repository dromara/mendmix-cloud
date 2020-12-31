/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.provider;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.cache.CacheExpires;
import com.jeesuite.cache.command.RedisBase;
import com.jeesuite.cache.command.RedisBatchCommand;
import com.jeesuite.cache.command.RedisObject;
import com.jeesuite.cache.command.RedisSortSet;
import com.jeesuite.cache.command.RedisStrList;
import com.jeesuite.cache.command.RedisString;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.mybatis.plugin.cache.CacheHandler;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月2日
 */
public class DefaultCacheProvider extends AbstractCacheProvider{

	protected static final Logger logger = LoggerFactory.getLogger("com.jeesuite.mybatis.plugin.cache");
	
	private boolean tenantModeEnabled = ResourceUtils.getBoolean("jeesuite.mybatis.tenantModeEnabled");
	private int batchSize = 100;
	
	public DefaultCacheProvider() {
		if(tenantModeEnabled && !ResourceUtils.getBoolean("jeesuite.cache.tenantModeEnabled")) {
			throw new RuntimeException("请配置[jeesuite.cache.tenantModeEnabled]启用缓存租户模式");
		}
	}

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
	public void putGroup(String cacheGroupKey, String key) {
		if(tenantModeEnabled) {
			key = RedisBase.buildTenantNameSpaceKey(key);
		}
		new RedisStrList(cacheGroupKey).lpush(key);
	}

	@Override
	public void clearGroup(final String groupName,String ...prefixs) {
		String cacheGroupKey = groupName.endsWith(CacheHandler.GROUPKEY_SUFFIX) ? groupName : groupName + CacheHandler.GROUPKEY_SUFFIX;
		RedisStrList redisList = new RedisStrList(cacheGroupKey);
		int keyCount = (int) redisList.length();
		if(keyCount <= 0)return;
	    //保护策略
		if(keyCount > 1000) {
			redisList.setExpire(CacheExpires.todayEndSeconds());
		}
		
		boolean withPrefixs = prefixs != null && prefixs.length > 0 && prefixs[0] != null;
		
		int toIndex;
		List<String> keys;
		for (int i = 0; i <= keyCount; i+=batchSize) {
			toIndex = (i + batchSize) > keyCount ? keyCount : (i + batchSize);
			keys = redisList.range(i, toIndex);
			if(keys.isEmpty())break;
			//
			if(withPrefixs) {
				keys = keys.stream().filter(key -> {
					for (String prefix : prefixs) {
						if(key.contains(prefix))return true;
					}
					return false;
				}).collect(Collectors.toList());
			}
			if(keys.isEmpty())continue;
			RedisBatchCommand.removeObjects(keys.toArray(new String[0]));
			if(logger.isDebugEnabled()) {
				logger.debug("_clearGroupKey -> group:{},keys:{}",groupName,Arrays.toString(keys.toArray()));
			}
		}
		redisList.remove();
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
