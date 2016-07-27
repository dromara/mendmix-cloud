package com.jeesuite.mybatis.crud.builder;

import static org.apache.ibatis.jdbc.SqlBuilder.BEGIN;
import static org.apache.ibatis.jdbc.SqlBuilder.DELETE_FROM;
import static org.apache.ibatis.jdbc.SqlBuilder.FROM;
import static org.apache.ibatis.jdbc.SqlBuilder.INSERT_INTO;
import static org.apache.ibatis.jdbc.SqlBuilder.SELECT;
import static org.apache.ibatis.jdbc.SqlBuilder.SET;
import static org.apache.ibatis.jdbc.SqlBuilder.SQL;
import static org.apache.ibatis.jdbc.SqlBuilder.UPDATE;
import static org.apache.ibatis.jdbc.SqlBuilder.VALUES;
import static org.apache.ibatis.jdbc.SqlBuilder.WHERE;

import java.util.Set;

import com.jeesuite.mybatis.crud.helper.ColumnMapper;
import com.jeesuite.mybatis.crud.helper.EntityMapper;
import com.jeesuite.mybatis.crud.helper.TableMapper;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月2日
 * @Copyright (c) 2015, jwww
 */
public class SqlBuilder {

	// sql占位符
	public static final String PLACEHOLDER = "@SELECT";
	//
	public static final String FROM = "FROM";

	/**
	 * 由传入的对象生成insert sql语句
	 * 
	 * @param tableMapper
	 * @param dto
	 * @return sql
	 * @throws Exception
	 */
	public static String buildInsertSql(EntityMapper entityMapper) {

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
			VALUES(columnMapper.getColumn(), "#{" + columnMapper.getProperty() + "}");
		}

		return SQL();
	}

	/**
	 * 由传入的对象生成update sql语句
	 * 
	 * @param object
	 * @return sql
	 * @throws Exception
	 */
	public static String buildUpdateSql(EntityMapper entityMapper) {

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
			SET(columnMapper.getColumn() + "=#{" + columnMapper.getProperty() + "}");
		}

		return SQL();
	}

	/**
	 * 由传入的对象生成delete sql语句
	 * 
	 * @param object
	 * @return sql
	 * @throws Exception
	 */
	public static String buildDeleteSql(EntityMapper entityMapper) {

		// 从表注解里获取表名等信息
		TableMapper tableMapper = entityMapper.getTableMapper();
		Set<ColumnMapper> idColumnMappers = entityMapper.getIdColumnsMapper();

		// 根据字段注解和属性值联合生成sql语句
		BEGIN();
		DELETE_FROM(tableMapper.getName());

		for (ColumnMapper columnMapper : idColumnMappers) {
			WHERE(columnMapper.getColumn() + "=#{" + columnMapper.getProperty() + "}");
		}

		return SQL();
	}

	/**
	 * 由传入的对象生成列字段 sql语句
	 * 
	 * @param entityMapper
	 * @return
	 */
	public static String buildColumSql(EntityMapper entityMapper) {
		// 从表注解里获取表名等信息
		Set<ColumnMapper> columnsMapper = entityMapper.getColumnsMapper();

		// 根据字段注解和属性值联合生成sql语句
		BEGIN();
		String table = entityMapper.getTableMapper().getName() + ".";
		for (ColumnMapper columnMapper : columnsMapper) {
			SELECT(table + columnMapper.getColumn());
		}

		return SQL();
	}

	/**
	 * 由传入的对象生成query sql语句
	 * 
	 * @param object
	 * @return sql
	 * @throws Exception
	 */
	public static String buildGetByIdSql(EntityMapper entityMapper) {

		// 从表注解里获取表名等信息
		TableMapper tableMapper = entityMapper.getTableMapper();
		Set<ColumnMapper> columnsMapper = entityMapper.getColumnsMapper();

		// 根据字段注解和属性值联合生成sql语句
		BEGIN();
		FROM(tableMapper.getName());

		for (ColumnMapper columnMapper : columnsMapper) {
			if (columnMapper.isId()) {
				WHERE(columnMapper.getColumn() + "=#{" + columnMapper.getProperty() + "}");
			}
			SELECT(columnMapper.getColumn());
		}

		return SQL();
	}

}
