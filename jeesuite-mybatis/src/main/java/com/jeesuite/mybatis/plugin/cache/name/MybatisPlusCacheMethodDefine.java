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
public class MybatisPlusCacheMethodDefine implements CacheMethodDefine {

	@Override
	public String selectName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String insertName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String updateName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String deleteName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String selectAllName() {
		return "selectAll";
	}

}
