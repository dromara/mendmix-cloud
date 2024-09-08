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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.dromara.mendmix.mybatis.metadata.ColumnMetadata;
import org.dromara.mendmix.mybatis.metadata.MetadataHelper;

/**
 * 
 * <br>
 * Class Name   : AbstractSelectMethodBuilder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月9日
 */
public abstract class AbstractSelectMethodBuilder extends AbstractMethodBuilder {

	@Override
	SqlCommandType sqlCommandType() {
		return SqlCommandType.SELECT;
	}

	@Override
	void setResultType(Configuration configuration, MappedStatement ms, Class<?> entityClass) {
       List<ResultMap> resultMaps = new ArrayList<ResultMap>();
       resultMaps.add(getResultMap(configuration,entityClass));
       MetaObject metaObject = SystemMetaObject.forObject(ms);
       metaObject.setValue("resultMaps", Collections.unmodifiableList(resultMaps));
	}
	
	/**
    * 生成当前实体的resultMap对象
    *
    * @param configuration
    * @return
    */
	public static ResultMap getResultMap(Configuration configuration,Class<?> entityClass) {
       List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
       
       Set<ColumnMetadata> entityClassColumns = MetadataHelper.getEntityMapper(entityClass).getColumnsMapper();
       for (ColumnMetadata entityColumn : entityClassColumns) {
           ResultMapping.Builder builder = new ResultMapping.Builder(configuration, entityColumn.getProperty(), entityColumn.getColumn(), entityColumn.getJavaType());
           if (entityColumn.getJdbcType() != null) {
               builder.jdbcType(entityColumn.getJdbcType());
           }

           List<ResultFlag> flags = new ArrayList<ResultFlag>();
           if (entityColumn.isId()) {
               flags.add(ResultFlag.ID);
           }
           builder.flags(flags);
           builder.lazy(false);
           resultMappings.add(builder.build());
       }
       ResultMap.Builder builder = new ResultMap.Builder(configuration, "BaseResultMap", entityClass, resultMappings, true);
       return builder.build();
   }

}
