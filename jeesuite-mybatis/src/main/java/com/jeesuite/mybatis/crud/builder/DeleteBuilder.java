/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import static org.apache.ibatis.jdbc.SqlBuilder.BEGIN;
import static org.apache.ibatis.jdbc.SqlBuilder.DELETE_FROM;
import static org.apache.ibatis.jdbc.SqlBuilder.SQL;
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
public class DeleteBuilder {

	/**
	 * @param configuration
	 * @param entity
	 */
	public static void build(Configuration configuration,LanguageDriver languageDriver, EntityInfo entity) {
		String msId = entity.getMapperClass().getName() + "." + GeneralSqlGenerator.methodDefines.deleteName();

		// 从参数对象里提取注解信息
		EntityMapper entityMapper = EntityHelper.getEntityMapper(entity.getEntityClass());
		// 生成sql
		String sql = buildDeleteSql(entityMapper);
		
		SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, entity.getEntityClass());
		
		MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, msId, sqlSource,SqlCommandType.DELETE);

		MappedStatement statement = statementBuilder.build();
		configuration.addMappedStatement(statement);
	}
	
	
	private static String buildDeleteSql(EntityMapper entityMapper) {

		// 从表注解里获取表名等信息
		TableMapper tableMapper = entityMapper.getTableMapper();
		ColumnMapper idColumn = entityMapper.getIdColumn();

		// 根据字段注解和属性值联合生成sql语句
		BEGIN();
		DELETE_FROM(tableMapper.getName());

		WHERE(idColumn.getColumn() + "=#{" + idColumn.getProperty() + "}");

		return String.format(SqlTemplate.SCRIPT_TEMAPLATE, SQL());
	}

}
