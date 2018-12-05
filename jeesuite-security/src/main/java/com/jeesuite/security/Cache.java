package com.jeesuite.security;

public interface Cache {

	void setString(String key,String value);
	String getString(String key);
	void setObject(String key,Object value);
	<T> T getObject(String key);
	void remove(String key);
	void removeAll();
	boolean exists(String key);
	
}
