/**
 * 
 */
package com.jeesuite.mybatis.core;

import java.io.Serializable;
import java.util.List;

import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

import com.jeesuite.mybatis.crud.provider.CountByExampleProvider;
import com.jeesuite.mybatis.crud.provider.SelectByExampleProvider;
import com.jeesuite.mybatis.crud.provider.UpdateWithVersionProvider;

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
	void insert(T entity);
	
	void insertSelective(T entity);
	
	/**
	  * @param entity
	*/
	int updateByPrimaryKey(T entity);

	/**
	  * @param entity
	*/
	int updateByPrimaryKeySelective(T entity);
	
	/**
	 * 带乐观锁更新
	 * @param entity
	*/
	@UpdateProvider(type = UpdateWithVersionProvider.class, method = "updateByPrimaryKeyWithVersion")
	@ResultType(int.class)
	int updateByPrimaryKeyWithVersion(T entity);
	

    /**
     * 获取指定的唯一标识符对应的持久化对象
     *
     * @param id 指定的唯一标识符
     * @return 指定的唯一标识符对应的持久化对象，如果没有对应的持久化对象，则返回null。
     */
	T selectByPrimaryKey(ID id);
	
	List<T> selectAll();

	void deleteByPrimaryKey(ID id);
	
	/**
	 * 批量插入
	 * @param entities
	 */
	void insertList(List<T> entities);
	
	long countAll();
	
	List<T> selectByPrimaryKeys(List<ID> ids);
	
	@SelectProvider(type = CountByExampleProvider.class, method = "countByExample")
	@ResultType(Long.class)
	long countByExample(T example);
	
	@SelectProvider(type = SelectByExampleProvider.class, method = "selectByExample")
	@ResultMap("BaseResultMap")
	List<T> selectByExample(T example);
	
}

