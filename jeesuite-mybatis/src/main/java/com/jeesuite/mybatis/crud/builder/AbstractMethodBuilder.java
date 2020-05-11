package com.jeesuite.mybatis.crud.builder;

import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.crud.helper.EntityHelper;
import com.jeesuite.mybatis.crud.helper.EntityMapper;
import com.jeesuite.mybatis.parser.EntityInfo;

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
	
	public void build(Configuration configuration, LanguageDriver languageDriver,EntityInfo entity) {
		
		for (String name : methodNames()) {			
			String msId = entity.getMapperClass().getName() + "." + name;
			
			// 从参数对象里提取注解信息
			EntityMapper entityMapper = EntityHelper.getEntityMapper(entity.getEntityClass());
			// 生成sql
			String sql = buildSQL(entityMapper,name.endsWith("Selective"));
			
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

	abstract SqlCommandType sqlCommandType();
	abstract String[] methodNames();
	abstract String buildSQL(EntityMapper entityMapper,boolean selective);
	abstract void setResultType(Configuration configuration, MappedStatement statement,Class<?> entityClass);
	
}
