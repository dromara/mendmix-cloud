/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import java.util.Set;

import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
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
			
			KeyGenerator keyGenerator = entityMapper.autoId() ? new Jdbc3KeyGenerator() : new NoKeyGenerator();
			statementBuilder.keyGenerator(keyGenerator)//
	                        .keyProperty(entityMapper.getIdColumn().getProperty())//
	                        .keyColumn(entityMapper.getIdColumn().getColumn());
	        
			MappedStatement statement = statementBuilder.build();
			
			configuration.addMappedStatement(statement);
		}
		
	}

	
	private static String buildInsertSql(EntityMapper entityMapper,boolean selective) {

		// 从表注解里获取表名等信息
		TableMapper table = entityMapper.getTableMapper();
		Set<ColumnMapper> columns = entityMapper.getColumnsMapper();

		StringBuilder fieldBuilder = new StringBuilder(SqlTemplate.TRIM_PREFIX);
		StringBuilder prppertyBuilder = new StringBuilder(SqlTemplate.TRIM_PREFIX);
		if (!entityMapper.autoId()) {
			/* 用户输入自定义ID */
			fieldBuilder.append(entityMapper.getIdColumn().getColumn()).append(",");
			prppertyBuilder.append("#{").append(entityMapper.getIdColumn().getProperty()).append("},");
		}
		for (ColumnMapper column : columns) {
			if (column.isId() || !column.isInsertable()) {
				continue;
			}
			String fieldExpr = SqlTemplate.wrapIfTag(column.getProperty(), column.getColumn(), !selective);
			String propertyExpr = SqlTemplate.wrapIfTag(column.getProperty(), "#{" + column.getProperty() + "}", !selective);
			fieldBuilder.append(fieldExpr);
			fieldBuilder.append(selective ? "\n" : ",");
			prppertyBuilder.append(propertyExpr);
			prppertyBuilder.append(selective ? "\n" : ",");
		}
		if(!selective){
			fieldBuilder.deleteCharAt(fieldBuilder.length() - 1);
			prppertyBuilder.deleteCharAt(prppertyBuilder.length() - 1);
		}
		fieldBuilder.append(SqlTemplate.TRIM_SUFFIX);
		prppertyBuilder.append(SqlTemplate.TRIM_SUFFIX);
		String sql = String.format(SqlTemplate.INSERT, table.getName(),fieldBuilder.toString(),prppertyBuilder.toString());
		return String.format(SqlTemplate.SCRIPT_TEMAPLATE, sql);
	}
	
	public static void main(String[] args) {
		String str = "<if test=\"password != null\">password</if>, <if test=\"type != null\">type</if>, <if test=\"email != null\">email</if>";
	    System.out.println(str.replaceAll(">,", ">"));
	}
	
}
