/**
 * 
 */
package com.jeesuite.mybatis.crud;

import java.util.List;

import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.crud.builder.BatchInsertBuilder;
import com.jeesuite.mybatis.crud.builder.DeleteBuilder;
import com.jeesuite.mybatis.crud.builder.GetByPrimaryKeyBuilder;
import com.jeesuite.mybatis.crud.builder.InsertBuilder;
import com.jeesuite.mybatis.crud.builder.UpdateBuilder;
import com.jeesuite.mybatis.crud.name.DefaultCrudMethodDefine;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年2月2日
 * @Copyright (c) 2015, jwww
 */
public class GeneralSqlGenerator {

	public static DefaultCrudMethodDefine methodDefines = new DefaultCrudMethodDefine();
	
	private LanguageDriver languageDriver;
	private Configuration configuration;
	
	public GeneralSqlGenerator(Configuration configuration) {
		this.configuration = configuration;
		this.languageDriver = configuration.getDefaultScriptingLanguageInstance();
	}
	public void generate() {
		if(languageDriver == null)languageDriver = configuration.getDefaultScriptingLanguageInstance();
		List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos();
		for (EntityInfo entity : entityInfos) {
			GetByPrimaryKeyBuilder.build(configuration, languageDriver,entity);
			InsertBuilder.build(configuration,languageDriver, entity);
			UpdateBuilder.build(configuration,languageDriver, entity);
			DeleteBuilder.build(configuration, languageDriver,entity);
			BatchInsertBuilder.build(configuration, languageDriver, entity);
		}
	}
}
