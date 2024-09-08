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

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.dromara.mendmix.mybatis.MybatisConfigs;
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
public class BatchLogicDeleteBuilder extends AbstractMethodBuilder{

	@Override
	SqlCommandType sqlCommandType() {
		return SqlCommandType.UPDATE;
	}

	@Override
	String[] methodNames() {
		return new String[]{"batchLogicDelete"};
	}

	@Override
	String buildSQL(EntityMetadata entityMapper, boolean selective) {

		// 从表注解里获取表名等信息
		TableMetadata tableMapper = entityMapper.getTableMapper();
		ColumnMetadata idColumn = entityMapper.getIdColumn();


		String updateColumnSet = "";
		ColumnMetadata updatedAtColumn = entityMapper.getColumnsMapper().stream().filter(o -> o.isUpdatedAtField()).findFirst().orElse(null);
		if(updatedAtColumn != null) {
			if(MybatisConfigs.DB_OFFSET == 0) {
				updateColumnSet = String.format(",%s=NOW()", updatedAtColumn.getColumn());
			}else {
				updateColumnSet = String.format(",%s=DATE_ADD(NOW(), INTERVAL %s HOUR)", updatedAtColumn.getColumn(),MybatisConfigs.DB_OFFSET);
			}
		}
		String sql = String.format(SqlTemplate.BATCH_DEL_BY_KEY, tableMapper.getName(),updateColumnSet,idColumn.getColumn());
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

