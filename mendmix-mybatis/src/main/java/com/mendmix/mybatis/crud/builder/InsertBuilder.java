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

import java.util.Set;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;

import com.mendmix.mybatis.crud.SqlTemplate;
import com.mendmix.mybatis.metadata.ColumnMetadata;
import com.mendmix.mybatis.metadata.EntityMetadata;
import com.mendmix.mybatis.metadata.TableMetadata;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月2日
 * @Copyright (c) 2015, jwww
 */
public class InsertBuilder  extends AbstractMethodBuilder{

	@Override
	SqlCommandType sqlCommandType() {
		return SqlCommandType.INSERT;
	}

	@Override
	String[] methodNames() {
		return new String[]{"insert","insertSelective"};
	}

	@Override
	String buildSQL(EntityMetadata entityMapper, boolean selective) {

		// 从表注解里获取表名等信息
		TableMetadata table = entityMapper.getTable();
		Set<ColumnMetadata> columns = entityMapper.getColumns();

		StringBuilder fieldBuilder = new StringBuilder(SqlTemplate.TRIM_PREFIX);
		StringBuilder prppertyBuilder = new StringBuilder(SqlTemplate.TRIM_PREFIX);
		if (!entityMapper.autoId()) {
			/* 用户输入自定义ID */
			fieldBuilder.append(entityMapper.getIdColumn().getColumn()).append(",");
			prppertyBuilder.append("#{").append(entityMapper.getIdColumn().getProperty()).append("},");
		}
		for (ColumnMetadata column : columns) {
			if (column.isId() || !column.isInsertable()) {
				continue;
			}
			String fieldExpr = SqlTemplate.wrapIfTag(column.getProperty(), column.getColumn(), !selective);
			String propertyExpr = SqlTemplate.wrapIfTag(column.getProperty(), "#{" + column.getProperty() + "}", !selective);
			fieldBuilder.append(fieldExpr);
			fieldBuilder.append(selective ? "\n" : ",");
			prppertyBuilder.append(propertyExpr);
			prppertyBuilder.append(selective ? "\n" : ",");
		}
		if(!selective){
			fieldBuilder.deleteCharAt(fieldBuilder.length() - 1);
			prppertyBuilder.deleteCharAt(prppertyBuilder.length() - 1);
		}
		fieldBuilder.append(SqlTemplate.TRIM_SUFFIX);
		prppertyBuilder.append(SqlTemplate.TRIM_SUFFIX);
		String sql = String.format(SqlTemplate.INSERT, table.getName(),fieldBuilder.toString(),prppertyBuilder.toString());
		return sql;
	}


	@Override
	void setResultType(Configuration configuration, MappedStatement statement, Class<?> entityClass) {}
	
	@Override
	boolean scriptWrapper() {
		return true;
	}
	
}

