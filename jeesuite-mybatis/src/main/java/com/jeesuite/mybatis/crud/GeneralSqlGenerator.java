/**
 * 
 */
package com.jeesuite.mybatis.crud;

import java.util.List;

import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.crud.builder.DeleteBuilder;
import com.jeesuite.mybatis.crud.builder.GetByPrimaryKeyBuilder;
import com.jeesuite.mybatis.crud.builder.InsertBuilder;
import com.jeesuite.mybatis.crud.builder.UpdateBuilder;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.cache.name.DefaultCacheMethodDefine;

/**
 * @description <br>
 * @author <a href="mailto:wei.jiang@lifesense.com">vakin</a>
 * @date 2016年3月24日
 * @Copyright (c) 2015, lifesense.com
 */
public class GeneralSqlGenerator {

	public static DefaultCacheMethodDefine methodDefines = new DefaultCacheMethodDefine();
	
	private LanguageDriver languageDriver;
	private Configuration configuration;
	
	public GeneralSqlGenerator(Configuration configuration) {
		this.configuration = configuration;
		this.languageDriver = configuration.getDefaultScriptingLanuageInstance();
	}
	public void generate() {
		if(languageDriver == null)languageDriver = configuration.getDefaultScriptingLanuageInstance();
		List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos();
		for (EntityInfo entity : entityInfos) {
			GetByPrimaryKeyBuilder.build(configuration, languageDriver,entity);
			InsertBuilder.build(configuration,languageDriver, entity);
			UpdateBuilder.build(configuration,languageDriver, entity);
			DeleteBuilder.build(configuration, languageDriver,entity);
		}
	}
}
