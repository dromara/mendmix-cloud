/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache;

import java.io.Closeable;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年3月23日
 * @Copyright (c) 2015, jwww
 */
public interface CacheProvider extends Closeable{

	<T> T get(String key);
	
	String getStr(String key);
	
	boolean set(String key,Object value,long expireSeconds);
	
	boolean setStr(String key,Object value,long expireSeconds);
	
	boolean remove(String key);
	
	void putGroup(String cacheGroupKey,String key,long expireSeconds);
	
	void removeFromGroup(String cacheGroupKey,String key);
	
	void clearExpiredGroupKeys(String cacheGroup);
	
	void clearGroup(String groupName,boolean containPkCache);
	
	boolean exists(String key);

}
