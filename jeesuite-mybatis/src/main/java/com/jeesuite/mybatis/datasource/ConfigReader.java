/**
 * 
 */
package com.jeesuite.mybatis.datasource;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月12日
 */
public interface ConfigReader {

	public String get(String key);
	
	public String getIfAbent(String key,Object defaulttVal);
	
	boolean containKey(String key);
}
