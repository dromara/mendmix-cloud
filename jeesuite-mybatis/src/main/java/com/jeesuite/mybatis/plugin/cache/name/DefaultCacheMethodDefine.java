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
		return "selectByKey";
	}

	@Override
	public String insertName() {
		return "insert";
	}

	@Override
	public String updateName() {
		return "updateByKey";
	}

	@Override
	public String deleteName() {
		return "deleteByKey";
	}

}
