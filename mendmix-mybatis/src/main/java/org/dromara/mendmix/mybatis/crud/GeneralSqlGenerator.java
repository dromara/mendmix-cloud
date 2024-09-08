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
package org.dromara.mendmix.mybatis.crud;

import java.util.List;

import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.dromara.mendmix.mybatis.crud.builder.BatchLogicDeleteBuilder;
import org.dromara.mendmix.mybatis.crud.builder.BatchUpdateByPrimaryKeysBuilder;
import org.dromara.mendmix.mybatis.crud.builder.CountAllBuilder;
import org.dromara.mendmix.mybatis.crud.builder.DeleteByPrimaryKeyBuilder;
import org.dromara.mendmix.mybatis.crud.builder.DeleteByPrimaryKeysBuilder;
import org.dromara.mendmix.mybatis.crud.builder.InsertBuilder;
import org.dromara.mendmix.mybatis.crud.builder.InsertListBuilder;
import org.dromara.mendmix.mybatis.crud.builder.SelectAllBuilder;
import org.dromara.mendmix.mybatis.crud.builder.SelectByPrimaryKeyBuilder;
import org.dromara.mendmix.mybatis.crud.builder.SelectByPrimaryKeysBuilder;
import org.dromara.mendmix.mybatis.crud.builder.UpdateBuilder;
import org.dromara.mendmix.mybatis.crud.builder.UpdateListByPrimaryKeysBuilder;
import org.dromara.mendmix.mybatis.crud.builder.UpdateListByPrimaryKeysSelectiveBuilder;
import org.dromara.mendmix.mybatis.kit.MybatisMapperParser;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		List<MapperMetadata> entityInfos = MybatisMapperParser.getMapperMetadatas(group);
		for (MapperMetadata entity : entityInfos) {
			new InsertBuilder().build(configuration, languageDriver,entity);
			new InsertListBuilder().build(configuration,languageDriver, entity);
			new DeleteByPrimaryKeyBuilder().build(configuration,languageDriver, entity);
			new UpdateBuilder().build(configuration,languageDriver, entity);
			new SelectAllBuilder().build(configuration, languageDriver,entity);
			new SelectByPrimaryKeyBuilder().build(configuration, languageDriver, entity);
			new SelectByPrimaryKeysBuilder().build(configuration, languageDriver, entity);
			new CountAllBuilder().build(configuration, languageDriver, entity);
			new BatchLogicDeleteBuilder().build(configuration, languageDriver, entity);
			new BatchUpdateByPrimaryKeysBuilder().build(configuration, languageDriver, entity);
			new UpdateListByPrimaryKeysBuilder().build(configuration, languageDriver, entity);
			new UpdateListByPrimaryKeysSelectiveBuilder().build(configuration, languageDriver, entity);
			new DeleteByPrimaryKeysBuilder().build(configuration, languageDriver, entity);
			log.info(">> generate autoCrud for:[{}] finish",entity.getEntityClass().getName());
		}
	}
}
