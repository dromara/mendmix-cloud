/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年3月23日
 * @Copyright (c) 2015, jwww
 */
public interface CacheProvider {

	<T> T get(String key);
	
	boolean set(String key,Object value,long expired);
	
	boolean remove(String key);
	
	void putGroupKeys(String groupKey,String subKey,long expireSeconds);
	
	void clearGroupKeys(String groupKey);
}
