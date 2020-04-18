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
	
	boolean exists(String key);
	
	void addZsetValue(String key,String value,double score);
	boolean existZsetValue(String key,String value);
	boolean removeZsetValue(String key,String value);
	boolean removeZsetValues(String key,double minScore, double maxScore);
	
	boolean setnx(String key,String value,long expireSeconds);
}
