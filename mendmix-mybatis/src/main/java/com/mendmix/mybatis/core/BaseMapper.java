/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.mybatis.core;

import java.io.Serializable;
import java.util.List;

import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

import com.mendmix.mybatis.crud.provider.CountByExampleProvider;
import com.mendmix.mybatis.crud.provider.DeleteByExampleProvider;
import com.mendmix.mybatis.crud.provider.SelectByExampleProvider;
import com.mendmix.mybatis.crud.provider.SelectOneByExampleProvider;
import com.mendmix.mybatis.crud.provider.UpdateWithVersionProvider;

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

	int deleteByPrimaryKey(ID id);
	
	int deleteByPrimaryKeys(List<ID> ids);
	
	/**
	 * 批量插入
	 * @param entities
	 */
	void insertList(List<T> entities);
	
	long countAll();
	
	List<T> selectByPrimaryKeys(List<ID> ids);
	
	@SelectProvider(type = CountByExampleProvider.class, method = "countByExample")
	@ResultType(Long.class)
	long countByExample(Object example);
	
	@SelectProvider(type = SelectByExampleProvider.class, method = "selectByExample")
	@ResultMap("BaseResultMap")
	List<T> selectByExample(Object example);
	
	int batchUpdateByPrimaryKeys(@Param("ids")List<ID> ids,@Param("example") T example);
	
	int updateListByPrimaryKeys(List<T> entities);
	
	/**
	 * 批量更新对象 （jdbc连接需指定allowMultiQueries=true）
	 * @param entities
	 * @return
	 */
	int updateListByPrimaryKeysSelective(List<T> entities);
	
	@DeleteProvider(type = DeleteByExampleProvider.class, method = "deleteByExample")
	@ResultType(Integer.class)
	Integer deleteByExample(Object example);
	
	@SelectProvider(type = SelectOneByExampleProvider.class, method = "selectOneByExample")
	@ResultMap("BaseResultMap")
	T selectOneByExample(Object example);
}

