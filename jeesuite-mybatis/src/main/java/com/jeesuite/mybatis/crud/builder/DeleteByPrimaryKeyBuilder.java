/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.metadata.ColumnMetadata;
import com.jeesuite.mybatis.metadata.EntityMetadata;
import com.jeesuite.mybatis.metadata.TableMetadata;

/**
 * 批量插入
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class DeleteByPrimaryKeyBuilder extends AbstractMethodBuilder{

	@Override
	SqlCommandType sqlCommandType() {
		return SqlCommandType.DELETE;
	}

	@Override
	String[] methodNames() {
		return new String[]{"deleteByPrimaryKey"};
	}

	@Override
	String buildSQL(EntityMetadata entityMapper, boolean selective) {
		// 从表注解里获取表名等信息
		TableMetadata tableMapper = entityMapper.getTable();
		ColumnMetadata idColumn = entityMapper.getIdColumn();
		SQL sql = new SQL().DELETE_FROM(tableMapper.getName()).WHERE(idColumn.getColumn() + "=#{" + idColumn.getProperty() + "}");
		return sql.toString();
	}

	@Override
	void setResultType(Configuration configuration, MappedStatement statement, Class<?> entityClass) {}

	@Override
	boolean scriptWrapper() {
		return false;
	}
	
}
