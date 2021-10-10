/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache;

import java.io.Closeable;
import java.util.List;

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
	
	boolean remove(String...keys);
	
	boolean exists(String key);
	
	void setExpire(String key,long expireSeconds);
	
    void putGroup(String cacheGroupKey,String key);
	
	void clearGroup(String groupName,String ...prefixs);
	
	List<String> getListItems(String key,int start,int end);
	
	long getListSize(String key);
	
	boolean setnx(String key,String value,long expireSeconds);
}
