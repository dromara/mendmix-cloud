package com.jeesuite.mybatis.crud.builder;

import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.crud.SqlTemplate;
import com.jeesuite.mybatis.metadata.EntityMetadata;
import com.jeesuite.mybatis.metadata.MapperMetadata;

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
