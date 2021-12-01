/**
 * 
 */
package com.jeesuite.mybatis.crud;

import java.util.List;

import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.mybatis.crud.builder.CountAllBuilder;
import com.jeesuite.mybatis.crud.builder.DeleteByPrimaryKeyBuilder;
import com.jeesuite.mybatis.crud.builder.InsertBuilder;
import com.jeesuite.mybatis.crud.builder.InsertListBuilder;
import com.jeesuite.mybatis.crud.builder.SelectAllBuilder;
import com.jeesuite.mybatis.crud.builder.SelectByPrimaryKeyBuilder;
import com.jeesuite.mybatis.crud.builder.SelectByPrimaryKeysBuilder;
import com.jeesuite.mybatis.crud.builder.UpdateBuilder;
import com.jeesuite.mybatis.metadata.MapperMetadata;
import com.jeesuite.mybatis.parser.MybatisMapperParser;

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
			log.info(" >> generate autoCrud for:[{}] finish",entity.getEntityClass().getName());
		}
	}
}
