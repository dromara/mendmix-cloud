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

import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

import com.mendmix.mybatis.crud.SqlTemplate;
import com.mendmix.mybatis.metadata.EntityMetadata;
import com.mendmix.mybatis.metadata.MapperMetadata;

/**
 * 
 * <br>
 * Class Name   : AbstractMethodBuilder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月9日
 */
public abstract class AbstractMethodBuilder extends AbstractExpressBuilder {
	
	public void build(Configuration configuration, LanguageDriver languageDriver,MapperMetadata mapperMeta) {
		
		for (String name : methodNames()) {			
			String msId = mapperMeta.getMapperClass().getName() + "." + name;
			
			// 从参数对象里提取注解信息
			EntityMetadata entityMapper = mapperMeta.getEntityMetadata();
			// 生成sql
			String sql = buildSQL(entityMapper,name.endsWith("Selective"));
			
			if(scriptWrapper()) {
				sql = String.format(SqlTemplate.SCRIPT_TEMAPLATE, sql);
			}
			
			SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, mapperMeta.getEntityClass());
			
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
			setResultType(configuration, statement, mapperMeta.getEntityClass());
			configuration.addMappedStatement(statement);
		}
		
	}

	abstract SqlCommandType sqlCommandType();
	abstract String[] methodNames();
	abstract String buildSQL(EntityMetadata entity,boolean selective);
	abstract boolean scriptWrapper();
	abstract void setResultType(Configuration configuration, MappedStatement statement,Class<?> entityClass);
	
}
