/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.crud.GeneralSqlGenerator;
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
public class GetByPrimaryKeyBuilder {

	/**
	 * @param configuration
	 * @param entity
	 */
	public static void build(Configuration configuration, EntityInfo entity) {
		String msId = entity.getMapperClass().getName() + "." + GeneralSqlGenerator.methodDefines.selectName();

		EntityMapper entityMapper = EntityHelper.getEntityMapper(entity.getEntityClass());

		String sql = SqlBuilder.buildGetByIdSql(entityMapper);
		DynamicSqlSource sqlSource = new DynamicSqlSource(configuration, MybatisObjectBuilder.generateSqlNode(sql));

		MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, msId, sqlSource,SqlCommandType.SELECT);

		// 将返回值修改为实体类型
		MappedStatement statement = statementBuilder.build();
		setResultType(configuration, statement, entity.getEntityClass());
		
		configuration.addMappedStatement(statement);
	}


	
	/**
	 * 设置返回值类型
	 *
	 * @param ms
	 * @param entityClass
	 */
	private static void setResultType(Configuration configuration, MappedStatement ms, Class<?> entityClass) {
        List<ResultMap> resultMaps = new ArrayList<ResultMap>();
        resultMaps.add(MybatisObjectBuilder.getResultMap(configuration,entityClass));
        MetaObject metaObject = SystemMetaObject.forObject(ms);
        metaObject.setValue("resultMaps", Collections.unmodifiableList(resultMaps));
	}
}
