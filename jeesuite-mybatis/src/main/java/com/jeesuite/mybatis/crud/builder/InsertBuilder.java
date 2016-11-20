/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import static org.apache.ibatis.jdbc.SqlBuilder.BEGIN;
import static org.apache.ibatis.jdbc.SqlBuilder.INSERT_INTO;
import static org.apache.ibatis.jdbc.SqlBuilder.SQL;
import static org.apache.ibatis.jdbc.SqlBuilder.VALUES;

import java.util.Set;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.crud.GeneralSqlGenerator;
import com.jeesuite.mybatis.crud.helper.ColumnMapper;
import com.jeesuite.mybatis.crud.helper.EntityHelper;
import com.jeesuite.mybatis.crud.helper.EntityMapper;
import com.jeesuite.mybatis.crud.helper.TableMapper;
import com.jeesuite.mybatis.parser.EntityInfo;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月2日
 * @Copyright (c) 2015, jwww
 */
public class InsertBuilder {

	/**
	 * @param configuration
	 * @param entity
	 */
	public static void build(Configuration configuration, LanguageDriver languageDriver,EntityInfo entity) {
		
		String[] names = GeneralSqlGenerator.methodDefines.insertName().split(",");
		for (String name : names) {			
			String msId = entity.getMapperClass().getName() + "." + name;
			
			// 从参数对象里提取注解信息
			EntityMapper entityMapper = EntityHelper.getEntityMapper(entity.getEntityClass());
			// 生成sql
			String sql = buildInsertSql(entityMapper,name.endsWith("Selective"));
			
			SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, entity.getEntityClass());
			
			MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, msId, sqlSource,SqlCommandType.INSERT);
			
//			statementBuilder.keyGenerator(keyGenerator)//
//	                        .keyProperty(keyProperty)//
//	                        .keyColumn(keyColumn);
	        
			MappedStatement statement = statementBuilder.build();
			
			configuration.addMappedStatement(statement);
		}
		
	}

	
	public static String buildInsertSql(EntityMapper entityMapper,boolean selective) {

		// 从表注解里获取表名等信息
		TableMapper tableMapper = entityMapper.getTableMapper();
		Set<ColumnMapper> columnMappers = entityMapper.getColumnsMapper();

		// 根据字段注解和属性值联合生成sql语句
		BEGIN();
		INSERT_INTO(tableMapper.getName());
		for (ColumnMapper columnMapper : columnMappers) {
			if (!columnMapper.isInsertable()) {
				continue;
			}
			
			String field = SqlTemplate.wrapIfTag(columnMapper.getProperty(), columnMapper.getColumn(), !selective);
			String value = SqlTemplate.wrapIfTag(columnMapper.getProperty(), "#{" + columnMapper.getProperty() + "}", !selective);
			VALUES(field, value);
		}
		
		return String.format(SqlTemplate.SCRIPT_TEMAPLATE, SQL());
	}
	
}
