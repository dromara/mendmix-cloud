package com.jeesuite.mybatis.plugin.cache;

import org.apache.ibatis.mapping.SqlCommandType;

/**
 * 按主键更新（add,update,delete）的缓存方法元信息
 * <br>
 * Class Name   : UpdateByPkMethodCache
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Dec 23, 2020
 */
public class UpdateByPkCacheMethodMetadata {
	public String keyPattern;
	
	public UpdateByPkCacheMethodMetadata(Class<?> entityClass,String methodName, String keyPattern, SqlCommandType sqlCommandType) {
		this.keyPattern = keyPattern;
	}
}
