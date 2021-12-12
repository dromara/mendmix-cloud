package com.jeesuite.cache;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface CacheAdapter {

	<T> T get(String key);
	
	String getString(String key);
	
	void set(String key,Object value,long expireSeconds);
	
	void setString(String key,Object value,long expireSeconds);
	
	void remove(String...keys);
	
	boolean exists(String key);

	void addListItems(String key,String ...items);
	
	List<String> getListItems(String key,int start,int end);
	
	long getListSize(String key);
	
	boolean setIfAbsent(String key,String value,long expireSeconds);
	
	void setMapItem(String key,String field,String value);
	
	Map<String, String> getMap(String key);
	
	String getMapItem(String key,String field);
	
	void setExpire(String key,long expireSeconds);
	
	void setExpireAt(String key,Date expireAt);
	
	long getTtl(String key);
}
