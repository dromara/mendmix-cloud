/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.mybatis.crud.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.dromara.mendmix.mybatis.crud.SqlTemplate;
import org.dromara.mendmix.mybatis.metadata.ColumnMetadata;
import org.dromara.mendmix.mybatis.metadata.EntityMetadata;
import org.dromara.mendmix.mybatis.metadata.TableMetadata;

/**
 * 批量插入
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class UpdateBuilder  extends AbstractMethodBuilder{

	@Override
	SqlCommandType sqlCommandType() {
		return SqlCommandType.UPDATE;
	}

	@Override
	String[] methodNames() {
		return new String[]{"updateByPrimaryKey","updateByPrimaryKeySelective"};
	}

	@Override
	String buildSQL(EntityMetadata entityMapper, boolean selective) {

		// 从表注解里获取表名等信息
		TableMetadata tableMapper = entityMapper.getTableMapper();
		Set<ColumnMetadata> columnMappers = entityMapper.getColumnsMapper();
		
		String idColumn = null;
		String idProperty = null;
		StringBuilder set = new StringBuilder();
		set.append("<trim prefix=\"SET\" suffixOverrides=\",\">");
		for (ColumnMetadata column : columnMappers) {
			if (!column.isUpdatable()) {
				continue;
			}
			if (column.isId()) {
				idColumn= column.getColumn();
				idProperty = column.getProperty();
			}else{
				String expr = SqlTemplate.wrapIfTag(column.getProperty(), column.getColumn() +"=#{"+column.getProperty()+"}", !selective);
				set.append(expr);
				if(!selective)set.append(",");
			}
		}
		if(!selective)set.deleteCharAt(set.length() - 1);
		set.append("</trim>");

		String sql = String.format(SqlTemplate.UPDATE_BY_KEY, tableMapper.getName(),set.toString(),idColumn,idProperty);

		return String.format(SqlTemplate.SCRIPT_TEMAPLATE, sql);
	}


	@Override
	void setResultType(Configuration configuration, MappedStatement statement, Class<?> entityClass) {
		ResultMap.Builder builder = new ResultMap.Builder(configuration, "int", Integer.class, new ArrayList<>(), true);
		MetaObject metaObject = SystemMetaObject.forObject(statement);
		List<ResultMap> resultMaps = Arrays.asList(builder.build());
		metaObject.setValue("resultMaps", resultMaps);
	}
	
}

