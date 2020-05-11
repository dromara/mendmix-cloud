/**
 * 
 */
package com.jeesuite.mybatis.crud;

/**
 * 定义按主键增删改查的方法名
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年9月16日
 */
public enum CrudMethods {
	selectByPrimaryKey,
	deleteByPrimaryKey,
	insert,
	insertSelective,
	insertList,
	updateByPrimaryKey,
	updateByPrimaryKeySelective,
	selectAll,
	selectByPrimaryKeys,
	selectByExample,
	countAll,
	countByExample
	;
}
