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
package com.mendmix.mybatis.crud.builder;

import org.apache.ibatis.jdbc.SQL;

import com.mendmix.mybatis.crud.CrudMethods;
import com.mendmix.mybatis.metadata.ColumnMetadata;
import com.mendmix.mybatis.metadata.EntityMetadata;
import com.mendmix.mybatis.metadata.TableMetadata;

/**
 * 批量插入
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class SelectByPrimaryKeyBuilder extends AbstractSelectMethodBuilder{

	@Override
	String[] methodNames() {
		return new String[]{CrudMethods.selectByPrimaryKey.name()};
	}

	@Override
	String buildSQL(EntityMetadata entityMapper, boolean selective) {
		// 从表注解里获取表名等信息
		TableMetadata tableMapper = entityMapper.getTable();
		ColumnMetadata idColumn = entityMapper.getIdColumn();
		
		return new SQL()
		   .SELECT("*")
		   .FROM(tableMapper.getName())
		   .WHERE(idColumn.getColumn() + "=#{" + idColumn.getProperty() + "}")
		   .toString();
	}
	
	@Override
	boolean scriptWrapper() {
		return false;
	}

}
