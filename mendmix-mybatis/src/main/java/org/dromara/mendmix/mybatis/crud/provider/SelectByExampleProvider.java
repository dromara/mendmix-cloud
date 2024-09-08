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
package org.dromara.mendmix.mybatis.crud.provider;

import java.util.Set;

import org.apache.ibatis.jdbc.SQL;
import org.dromara.mendmix.mybatis.metadata.ColumnMetadata;
import org.dromara.mendmix.mybatis.metadata.EntityMetadata;
import org.dromara.mendmix.mybatis.metadata.MetadataHelper;

/**
 * 
 * <br>
 * Class Name   : CountByExampleProvider
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月11日
 */
public class SelectByExampleProvider extends AbstractExampleProvider{

	public String selectByExample(Object example) throws Exception {
		EntityMetadata entityMapper = currentEntityMetadata(example);
		Set<ColumnMetadata> columns = entityMapper.getColumnsMapper();
		SQL sql = new SQL().SELECT("*").FROM(entityMapper.getTableMapper().getName());
		Object value;
		StringBuilder whereBuilder = new StringBuilder();
		for (ColumnMetadata column : columns) {
			value = MetadataHelper.getEntityField(entityMapper.getTableMapper().getName(),column.getProperty()).get(example);
			if(value == null)continue;
			appendWhere(whereBuilder,column);
		}
		if(whereBuilder.length() == 0) {
			throw new IllegalArgumentException("至少包含一个查询条件");
		}
		sql.WHERE(whereBuilder.toString());
		return sql.toString();
	}

	
}
