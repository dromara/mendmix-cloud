/**
 * 
 */
package com.jeesuite.cache.local;

import java.io.Closeable;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月4日
 */
public interface Level1CacheProvider extends Closeable{

	void start();
	boolean set(String cacheName,String key,Object value);
	
    <T> T get(String cacheName,String key);
    
    void remove(String cacheName,String key);
    
    void remove(String cacheName);
    
    void clearAll();
    
}
