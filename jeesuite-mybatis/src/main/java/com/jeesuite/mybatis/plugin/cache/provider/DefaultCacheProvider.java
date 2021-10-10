/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.provider;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.cache.command.RedisBase;
import com.jeesuite.cache.command.RedisBatchCommand;
import com.jeesuite.cache.command.RedisList;
import com.jeesuite.cache.command.RedisObject;
import com.jeesuite.cache.command.RedisStrList;
import com.jeesuite.cache.command.RedisString;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.mybatis.MybatisConfigs;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月2日
 */
public class DefaultCacheProvider extends AbstractCacheProvider{

	protected static final Logger logger = LoggerFactory.getLogger("com.jeesuite.mybatis.plugin.cache");
	
	public DefaultCacheProvider(String groupName) {
		super(groupName);
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
	public boolean remove(String... keys) {
		if(keys.length == 1) {
			return new RedisObject(keys[0]).remove();
		}
		return RedisBatchCommand.removeObjects(keys);
	}

	@Override
	public void setExpire(String key, long expireSeconds) {
		new RedisObject(key).setExpire(expireSeconds);
	}

	@Override
	public List<String> getListItems(String key, int start, int end) {
		return new RedisList(key).range(start, end);
	}
	
	@Override
	public long getListSize(String key) {
		return new RedisList(key).length();
	}

	@Override
	public boolean exists(String key) {
		return new RedisObject(key).exists();
	}
	
	@Override
	public void putGroup(String cacheGroupKey, String key) {
		if(MybatisConfigs.isSchameSharddingTenant(groupName)) {
			key = RedisBase.buildTenantNameSpaceKey(key);
		}
		new RedisStrList(cacheGroupKey).lpush(key);
	}


	@Override
	public boolean setnx(String key, String value, long expireSeconds) {
		return new RedisString(key).setnx(value, expireSeconds);
	}
	
	@Override
	public void close() throws IOException {}

	

}
