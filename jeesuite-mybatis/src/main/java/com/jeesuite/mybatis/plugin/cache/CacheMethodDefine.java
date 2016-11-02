/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache;

/**
 * 定义按主键增删改查的方法名
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年9月16日
 */
public interface CacheMethodDefine {

	String selectName();
	
	String insertName();
	
	String updateName();
	
	String deleteName();
	
	String selectAllName();
}
