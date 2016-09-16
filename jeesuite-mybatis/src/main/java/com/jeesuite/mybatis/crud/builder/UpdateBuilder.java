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
public class UpdateBuilder {

	/**
	 * @param configuration
	 * @param entity
	 */
	public static void build(Configuration configuration, EntityInfo entity) {
		String msId = entity.getMapperClass().getName() + ".updateByKey";

		EntityMapper entityMapper = EntityHelper.getEntityMapper(entity.getEntityClass());

		String sql = SqlBuilder.buildUpdateSql(entityMapper);
		DynamicSqlSource sqlSource = new DynamicSqlSource(configuration, MybatisObjectBuilder.generateSqlNode(sql));

		MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, msId, sqlSource,SqlCommandType.UPDATE);

		// 将返回值修改为实体类型
		MappedStatement statement = statementBuilder.build();
		
		configuration.addMappedStatement(statement);
	}

}
