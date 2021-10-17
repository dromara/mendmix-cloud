package com.jeesuite.security;

public interface Cache {

	void setString(String key,String value);
	String getString(String key);
	void setObject(String key,Object value);
	<T> T getObject(String key);
	void remove(String key);
	void removeAll();
	boolean exists(String key);
	void setMapValue(String key,String field,Object value);
	<T> T getMapValue(String key,String field);
	void updateExpireTime(String key);
	
}
