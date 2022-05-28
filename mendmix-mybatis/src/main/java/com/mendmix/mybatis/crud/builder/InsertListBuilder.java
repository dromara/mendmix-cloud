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
 * 批量插入
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class InsertListBuilder extends AbstractMethodBuilder{

	@Override
	SqlCommandType sqlCommandType() {
		return SqlCommandType.INSERT;
	}

	@Override
	String[] methodNames() {
		return new String[]{"insertList"};
	}

	@Override
	String buildSQL(EntityMetadata entityMapper, boolean selective) {

		// 从表注解里获取表名等信息
		TableMetadata table = entityMapper.getTable();
		Set<ColumnMetadata> columns = entityMapper.getColumns();

		StringBuilder fieldBuilder = new StringBuilder("(");
		StringBuilder prppertyBuilder = new StringBuilder("(");
		if (!entityMapper.autoId()) {
			fieldBuilder.append(entityMapper.getIdColumn().getColumn()).append(",");
			prppertyBuilder.append("#{item.").append(entityMapper.getIdColumn().getProperty()).append("},");
		}
		for (ColumnMetadata column : columns) {
			if (column.isId() || !column.isInsertable()) {
				continue;
			}
			String fieldExpr = SqlTemplate.wrapIfTag(column.getProperty(), column.getColumn(), true);
			String propertyExpr = SqlTemplate.wrapIfTag(column.getProperty(), "#{item." + column.getProperty() + "}", true);
			fieldBuilder.append(fieldExpr);
			fieldBuilder.append(",");
			prppertyBuilder.append(propertyExpr);
			prppertyBuilder.append(",");
		}
		
		fieldBuilder.deleteCharAt(fieldBuilder.length() - 1);
		prppertyBuilder.deleteCharAt(prppertyBuilder.length() - 1);
		
		fieldBuilder.append(")");
		prppertyBuilder.append(")");
		String sql = String.format(SqlTemplate.BATCH_INSERT, table.getName(),fieldBuilder.toString(),prppertyBuilder.toString());
		return sql;
	}

	@Override
	void setResultType(Configuration configuration, MappedStatement statement, Class<?> entityClass) {}
	
	@Override
	boolean scriptWrapper() {
		return true;
	}
}
