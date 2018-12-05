/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
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
public class SelectAllBuilder {

	/**
	 * @param configuration
	 * @param entity
	 */
	public static void build(Configuration configuration, LanguageDriver languageDriver,EntityInfo entity) {
		String msId = entity.getMapperClass().getName() + "." + GeneralSqlGenerator.methodDefines.selectAllName();

		EntityMapper entityMapper = EntityHelper.getEntityMapper(entity.getEntityClass());

		String sql = buildSelectAllSql(entityMapper);

		SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, entity.getEntityClass());
		
		MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, msId, sqlSource,SqlCommandType.SELECT);

		// 将返回值修改为实体类型
		MappedStatement statement = statementBuilder.build();
		setResultType(configuration, statement, entity.getEntityClass());
		
		configuration.addMappedStatement(statement);
	}

	
	private static String buildSelectAllSql(EntityMapper entityMapper) {
		TableMapper tableMapper = entityMapper.getTableMapper();
		return new SQL() {
            {
                SELECT("*");
                FROM(tableMapper.getName());
            }
        }.toString();
	}

	
	/**
	 * 设置返回值类型
	 *
	 * @param ms
	 * @param entityClass
	 */
	private static void setResultType(Configuration configuration, MappedStatement ms, Class<?> entityClass) {
        List<ResultMap> resultMaps = new ArrayList<ResultMap>();
        resultMaps.add(getResultMap(configuration,entityClass));
        MetaObject metaObject = SystemMetaObject.forObject(ms);
        metaObject.setValue("resultMaps", Collections.unmodifiableList(resultMaps));
	}
	
	/**
     * 生成当前实体的resultMap对象
     *
     * @param configuration
     * @return
     */
	public static ResultMap getResultMap(Configuration configuration,Class<?> entityClass) {
        List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
        
        Set<ColumnMapper> entityClassColumns = EntityHelper.getEntityMapper(entityClass).getColumnsMapper();
        for (ColumnMapper entityColumn : entityClassColumns) {
            ResultMapping.Builder builder = new ResultMapping.Builder(configuration, entityColumn.getProperty(), entityColumn.getColumn(), entityColumn.getJavaType());
            if (entityColumn.getJdbcType() != null) {
                builder.jdbcType(entityColumn.getJdbcType());
            }

            List<ResultFlag> flags = new ArrayList<ResultFlag>();
            if (entityColumn.isId()) {
                flags.add(ResultFlag.ID);
            }
            builder.flags(flags);
            builder.lazy(false);
            resultMappings.add(builder.build());
        }
        ResultMap.Builder builder = new ResultMap.Builder(configuration, "BaseResultMap", entityClass, resultMappings, true);
        return builder.build();
    }
}
