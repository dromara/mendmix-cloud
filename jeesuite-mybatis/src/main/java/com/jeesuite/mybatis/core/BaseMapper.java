/**
 * 
 */
package com.jeesuite.mybatis.core;

import java.io.Serializable;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月2日
 * @Copyright (c) 2015, jwww
 */
public abstract interface BaseMapper<T extends BaseEntity, ID extends Serializable> {

    /**
     * 保存（持久化）对象
     * @param entity 要持久化的对象
     */
	public void insert(T entity);
	
	public void insertSelective(T entity);
	
	/**
	  * @param entity
	*/
	public void update(T entity);

	/**
	  * @param entity
	*/
	public void updateSelective(T entity);
	

    /**
     * 获取指定的唯一标识符对应的持久化对象
     *
     * @param id 指定的唯一标识符
     * @return 指定的唯一标识符对应的持久化对象，如果没有对应的持久化对象，则返回null。
     */
	public T getByKey(ID id);

	void deleteByKey(ID id);
}

