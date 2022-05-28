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
package com.mendmix.mybatis.crud.provider;

import java.util.Set;

import org.apache.ibatis.jdbc.SQL;

import com.mendmix.mybatis.crud.builder.AbstractExpressBuilder;
import com.mendmix.mybatis.metadata.ColumnMetadata;
import com.mendmix.mybatis.metadata.EntityMetadata;
import com.mendmix.mybatis.metadata.MetadataHelper;

/**
 * 
 * <br>
 * Class Name : CountByExampleProvider
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月11日
 */
public class CountByExampleProvider extends AbstractExpressBuilder{

	public String countByExample(Object example) throws Exception {
		EntityMetadata entityMapper = MetadataHelper.getEntityMapper(example.getClass());
		Set<ColumnMetadata> columns = entityMapper.getColumns();
		SQL sql = new SQL().SELECT("COUNT(1)").FROM(entityMapper.getTable().getName());
		Object value;
		StringBuilder whereBuilder = new StringBuilder();
		for (ColumnMetadata column : columns) {
			value = MetadataHelper.getEntityField(entityMapper.getTable().getName(),column.getProperty()).get(example);
			if(value == null)continue;
			appendWhere(whereBuilder,column);
		}
		if(whereBuilder.length() == 0)throw new IllegalArgumentException("至少包含一个查询条件");
		
		sql.WHERE(whereBuilder.toString());
		return sql.toString();
	}
}
