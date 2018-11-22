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
 * 批量插入
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class BatchInsertBuilder {

	/**
	 * @param configuration
	 * @param entity
	 */
	public static void build(Configuration configuration, LanguageDriver languageDriver,EntityInfo entity) {
		
		
		String msId = entity.getMapperClass().getName() + ".insertList";
		
		// 从参数对象里提取注解信息
		EntityMapper entityMapper = EntityHelper.getEntityMapper(entity.getEntityClass());
		// 生成sql
		String sql = buildBatchInsertSql(entityMapper);
		
		SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, entity.getEntityClass());
		
		MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, msId, sqlSource,SqlCommandType.INSERT);
		
		KeyGenerator keyGenerator = entityMapper.autoId() ? new Jdbc3KeyGenerator() : new NoKeyGenerator();
		statementBuilder.keyGenerator(keyGenerator)//
                        .keyProperty(entityMapper.getIdColumn().getProperty())//
                        .keyColumn(entityMapper.getIdColumn().getColumn());
        
		MappedStatement statement = statementBuilder.build();
		
		configuration.addMappedStatement(statement);
	
		
	}

	
	private static String buildBatchInsertSql(EntityMapper entityMapper) {

		// 从表注解里获取表名等信息
		TableMapper table = entityMapper.getTableMapper();
		Set<ColumnMapper> columns = entityMapper.getColumnsMapper();

		StringBuilder fieldBuilder = new StringBuilder("(");
		StringBuilder prppertyBuilder = new StringBuilder("(");
		if (!entityMapper.autoId()) {
			fieldBuilder.append(entityMapper.getIdColumn().getColumn()).append(",");
			prppertyBuilder.append("#{item.").append(entityMapper.getIdColumn().getProperty()).append("},");
		}
		for (ColumnMapper column : columns) {
			if (column.isId() || !column.isInsertable()) {
				continue;
			}
			String fieldExpr = SqlTemplate.wrapIfTag(column.getProperty(), column.getColumn(), true);
			String propertyExpr = SqlTemplate.wrapIfTag(column.getProperty(), "#{item." + column.getProperty() + "}", true);
			fieldBuilder.append(fieldExpr);
			fieldBuilder.append(",");
			prppertyBuilder.append(propertyExpr);
			prppertyBuilder.append(",");
		}
		
		fieldBuilder.deleteCharAt(fieldBuilder.length() - 1);
		prppertyBuilder.deleteCharAt(prppertyBuilder.length() - 1);
		
		fieldBuilder.append(")");
		prppertyBuilder.append(")");
		String sql = String.format(SqlTemplate.BATCH_INSERT, table.getName(),fieldBuilder.toString(),prppertyBuilder.toString());
		return String.format(SqlTemplate.SCRIPT_TEMAPLATE, sql);
	}
	
	public static void main(String[] args) {
		String str = "<if test=\"password != null\">password</if>, <if test=\"type != null\">type</if>, <if test=\"email != null\">email</if>";
	    System.out.println(str.replaceAll(">,", ">"));
	}
	
}
