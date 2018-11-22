/**
 * 
 */
package com.jeesuite.mybatis.crud.name;

import com.jeesuite.mybatis.crud.CrudMethodDefine;

/**
 * Mapper3 框架方法定义
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年9月16日
 */
public class Mapper3CrudMethodDefine implements CrudMethodDefine {

	@Override
	public String selectName() {
		return "selectByPrimaryKey";
	}

	@Override
	public String insertName() {
		return "insert,insertSelective";
	}

	@Override
	public String updateName() {
		return "updateByPrimaryKey,updateByPrimaryKeySelective";
	}

	@Override
	public String deleteName() {
		return "deleteByPrimaryKey";
	}

	@Override
	public String selectAllName() {
		return "selectAll";
	}

}
