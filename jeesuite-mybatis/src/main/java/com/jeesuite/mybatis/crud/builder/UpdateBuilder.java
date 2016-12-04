/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

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
public class UpdateBuilder {

	/**
	 * @param configuration
	 * @param entity
	 */
	public static void build(Configuration configuration,LanguageDriver languageDriver, EntityInfo entity) {
		String[] names = GeneralSqlGenerator.methodDefines.updateName().split(",");
		for (String name : names) {			
			String msId = entity.getMapperClass().getName() + "." + name;
			
			EntityMapper entityMapper = EntityHelper.getEntityMapper(entity.getEntityClass());
			
			String sql = buildUpdateSql(entityMapper,name.endsWith("Selective"));
			
			SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, entity.getEntityClass());
			
			MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, msId, sqlSource,SqlCommandType.UPDATE);
			
			// 将返回值修改为实体类型
			MappedStatement statement = statementBuilder.build();
			
			configuration.addMappedStatement(statement);
		}
	}
	
	
	public static String buildUpdateSql(EntityMapper entityMapper,boolean selective) {

		// 从表注解里获取表名等信息
		TableMapper tableMapper = entityMapper.getTableMapper();
		Set<ColumnMapper> columnMappers = entityMapper.getColumnsMapper();
		
		String idColumn = null;
		String idProperty = null;
		StringBuilder set = new StringBuilder();
		set.append("<trim prefix=\"SET\" suffixOverrides=\",\">");
		for (ColumnMapper column : columnMappers) {
			if (!column.isUpdatable()) {
				continue;
			}
			if (column.isId()) {
				idColumn= column.getColumn();
				idProperty = column.getProperty();
			}
			String expr = SqlTemplate.wrapIfTag(column.getProperty(), column.getColumn() +"=#{"+column.getProperty()+"}", !selective);
			set.append(expr);
			if(!selective)set.append(",");
		}
		if(!selective)set.deleteCharAt(set.length() - 1);
		set.append("</trim>");

		String sql = String.format(SqlTemplate.UPDATE_BY_KEY, tableMapper.getName(),set.toString(),idColumn,idProperty);

		return String.format(SqlTemplate.SCRIPT_TEMAPLATE, sql);
	}


}
