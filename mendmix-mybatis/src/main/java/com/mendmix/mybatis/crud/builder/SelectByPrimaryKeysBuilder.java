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

import com.mendmix.mybatis.crud.CrudMethods;
import com.mendmix.mybatis.crud.SqlTemplate;
import com.mendmix.mybatis.metadata.ColumnMetadata;
import com.mendmix.mybatis.metadata.EntityMetadata;
import com.mendmix.mybatis.metadata.TableMetadata;

/**
 * 
 * <br>
 * Class Name   : SelectByIdsBuilder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月9日
 */
public class SelectByPrimaryKeysBuilder extends AbstractSelectMethodBuilder{

	@Override
	String[] methodNames() {
		return new String[]{CrudMethods.selectByPrimaryKeys.name()};
	}

	@Override
	String buildSQL(EntityMetadata entityMapper, boolean selective) {
		TableMetadata tableMapper = entityMapper.getTable();
		ColumnMetadata idColumn = entityMapper.getIdColumn();
		String sql = String.format(SqlTemplate.SELECT_BY_KEYS, tableMapper.getName(),idColumn.getColumn());
		return sql;
	}
	
	@Override
	boolean scriptWrapper() {
		return true;
	}

}
