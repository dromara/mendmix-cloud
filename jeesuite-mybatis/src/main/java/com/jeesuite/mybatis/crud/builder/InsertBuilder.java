/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.crud.helper.EntityHelper;
import com.jeesuite.mybatis.crud.helper.EntityMapper;
import com.jeesuite.mybatis.crud.helper.MybatisObjectBuilder;
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
	public static void build(Configuration configuration, EntityInfo entity) {
		String msId = entity.getMapperClass().getName() + ".insert";

		// 从参数对象里提取注解信息
		EntityMapper entityMapper = EntityHelper.getEntityMapper(entity.getEntityClass());
	    // 生成sql
		String sql = SqlBuilder.buildInsertSql(entityMapper);
		
		DynamicSqlSource sqlSource = new DynamicSqlSource(configuration, MybatisObjectBuilder.generateSqlNode(sql));

		MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, msId, sqlSource,SqlCommandType.INSERT);

		MappedStatement statement = statementBuilder.build();
		
		configuration.addMappedStatement(statement);
	}

	
}
