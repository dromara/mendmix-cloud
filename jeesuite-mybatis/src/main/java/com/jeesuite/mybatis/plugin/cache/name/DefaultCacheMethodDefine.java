/**
 * 
 */
package com.jeesuite.mybatis.plugin.cache.name;

import com.jeesuite.mybatis.plugin.cache.CacheMethodDefine;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年9月16日
 */
public class DefaultCacheMethodDefine implements CacheMethodDefine {

	@Override
	public String selectName() {
		return "getByKey";
	}

	@Override
	public String insertName() {
		return "insert,insertSelective";
	}

	@Override
	public String updateName() {
		return "updateByKey,updateByKeySelective";
	}

	@Override
	public String deleteName() {
		return "deleteByKey";
	}
	
	@Override
	public String selectAllName() {
		return "selectAll";
	}

}
