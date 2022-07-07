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
package com.mendmix.mybatis.crud;

import java.util.List;

import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.mybatis.crud.builder.BatchUpdateByPrimaryKeysBuilder;
import com.mendmix.mybatis.crud.builder.CountAllBuilder;
import com.mendmix.mybatis.crud.builder.DeleteByPrimaryKeyBuilder;
import com.mendmix.mybatis.crud.builder.InsertBuilder;
import com.mendmix.mybatis.crud.builder.InsertListBuilder;
import com.mendmix.mybatis.crud.builder.SelectAllBuilder;
import com.mendmix.mybatis.crud.builder.SelectByPrimaryKeyBuilder;
import com.mendmix.mybatis.crud.builder.SelectByPrimaryKeysBuilder;
import com.mendmix.mybatis.crud.builder.UpdateBuilder;
import com.mendmix.mybatis.metadata.MapperMetadata;
import com.mendmix.mybatis.parser.MybatisMapperParser;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年2月2日
 * @Copyright (c) 2015, jwww
 */
public class GeneralSqlGenerator {

	private static final Logger log = LoggerFactory.getLogger(GeneralSqlGenerator.class);
	
	private LanguageDriver languageDriver;
	private Configuration configuration;
	private String group;
	
	public GeneralSqlGenerator(String group,Configuration configuration) {
		this.group = group;
		this.configuration = configuration;
		this.languageDriver = configuration.getDefaultScriptingLanguageInstance();
	}
	
	public void generate() {
		if(languageDriver == null)languageDriver = configuration.getDefaultScriptingLanguageInstance();
		List<MapperMetadata> mappers = MybatisMapperParser.getMapperMetadatas(group);
		for (MapperMetadata entity : mappers) {
			entity.setGroup(group);
			new InsertBuilder().build(configuration, languageDriver,entity);
			new InsertListBuilder().build(configuration,languageDriver, entity);
			new DeleteByPrimaryKeyBuilder().build(configuration,languageDriver, entity);
			new UpdateBuilder().build(configuration,languageDriver, entity);
			new SelectAllBuilder().build(configuration, languageDriver,entity);
			new SelectByPrimaryKeyBuilder().build(configuration, languageDriver, entity);
			new SelectByPrimaryKeysBuilder().build(configuration, languageDriver, entity);
			new CountAllBuilder().build(configuration, languageDriver, entity);
			new BatchUpdateByPrimaryKeysBuilder().build(configuration, languageDriver, entity);
			log.info("MENDMIX-TRACE-LOGGGING-->> generate autoCrud for:[{}] finish",entity.getEntityClass().getName());
		}
	}
}
