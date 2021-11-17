/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.crud.CrudMethods;
import com.jeesuite.mybatis.metadata.EntityMetadata;
import com.jeesuite.mybatis.metadata.TableMetadata;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月2日
 * @Copyright (c) 2015, jwww
 */
public class CountAllBuilder  extends AbstractMethodBuilder{

	@Override
	String[] methodNames() {
		return new String[]{CrudMethods.countAll.name()};
	}
	
	@Override
	SqlCommandType sqlCommandType() {
		return SqlCommandType.SELECT;
	}

	@Override
	String buildSQL(EntityMetadata entityMapper, boolean selective) {
		TableMetadata tableMapper = entityMapper.getTable();
		return new SQL().SELECT("COUNT(1)").FROM(tableMapper.getName()).toString();
	}

	@Override
	void setResultType(Configuration configuration, MappedStatement statement, Class<?> entityClass) {
		ResultMap.Builder builder = new ResultMap.Builder(configuration, "long", Long.class, new ArrayList<>(), true);
		MetaObject metaObject = SystemMetaObject.forObject(statement);
		List<ResultMap> resultMaps = Arrays.asList(builder.build());
		metaObject.setValue("resultMaps", resultMaps);
	}

}
