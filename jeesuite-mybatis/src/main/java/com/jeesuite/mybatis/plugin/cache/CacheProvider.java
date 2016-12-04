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
	
	boolean set(String key,Object value,long expired);
	
	boolean remove(String key);
	
	void putGroupKeys(String cacheGroup,String subKey,long expireSeconds);
	
	void clearGroupKeys(String cacheGroup);
	
	void clearGroupKey(String cacheGroup,String subKey);
	
	void clearExpiredGroupKeys(String cacheGroup);

}
