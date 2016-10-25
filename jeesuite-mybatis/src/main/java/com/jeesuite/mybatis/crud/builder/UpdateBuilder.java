/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import static org.apache.ibatis.jdbc.SqlBuilder.BEGIN;
import static org.apache.ibatis.jdbc.SqlBuilder.SET;
import static org.apache.ibatis.jdbc.SqlBuilder.SQL;
import static org.apache.ibatis.jdbc.SqlBuilder.UPDATE;
import static org.apache.ibatis.jdbc.SqlBuilder.WHERE;

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

		// 根据字段注解和属性值联合生成sql语句
		BEGIN();
		UPDATE(tableMapper.getName());

		for (ColumnMapper columnMapper : columnMappers) {
			if (columnMapper.isId()) {
				WHERE(columnMapper.getColumn() + "=#{" + columnMapper.getProperty() + "}");
				continue;
			}
			if (!columnMapper.isUpdatable()) {
				continue;
			}
			String setContent = SqlTemplate.wrapIfTag(columnMapper.getProperty(), columnMapper.getColumn() + "=#{" + columnMapper.getProperty() + "}", !selective);
			SET(setContent);
		}

		return String.format(SqlTemplate.SCRIPT_TEMAPLATE, SQL());
	}


}
