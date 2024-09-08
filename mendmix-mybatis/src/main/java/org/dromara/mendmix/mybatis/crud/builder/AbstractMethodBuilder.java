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

import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.dromara.mendmix.mybatis.metadata.EntityMetadata;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.dromara.mendmix.mybatis.metadata.MetadataHelper;

/**
 * 
 * <br>
 * Class Name   : AbstractMethodBuilder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月9日
 */
public abstract class AbstractMethodBuilder {
	
	public void build(Configuration configuration, LanguageDriver languageDriver,MapperMetadata entity) {
		
		for (String name : methodNames()) {			
			String msId = entity.getMapperClass().getName() + "." + name;
			
			// 从参数对象里提取注解信息
			EntityMetadata entityMapper = MetadataHelper.getEntityMapper(entity.getEntityClass());
			// 生成sql
			boolean selective = selective() ? true : name.endsWith("Selective");
			String sql = buildSQL(entityMapper,selective);
			
			SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, entity.getEntityClass());
			
			MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, msId, sqlSource,sqlCommandType());
			
			//主键策略
			if(sqlCommandType() == SqlCommandType.INSERT){
				KeyGenerator keyGenerator = entityMapper.autoId() ? new Jdbc3KeyGenerator() : new NoKeyGenerator();
				statementBuilder.keyGenerator(keyGenerator)//
		                        .keyProperty(entityMapper.getIdColumn().getProperty())//
		                        .keyColumn(entityMapper.getIdColumn().getColumn());
			}
			
			MappedStatement statement = statementBuilder.build();
			//
			setResultType(configuration, statement, entity.getEntityClass());
			configuration.addMappedStatement(statement);
		}
		
	}
	
	protected boolean selective() {
		return false;
	}

	abstract SqlCommandType sqlCommandType();
	abstract String[] methodNames();
	abstract String buildSQL(EntityMetadata entityMapper,boolean selective);
	abstract void setResultType(Configuration configuration, MappedStatement statement,Class<?> entityClass);
	
}
