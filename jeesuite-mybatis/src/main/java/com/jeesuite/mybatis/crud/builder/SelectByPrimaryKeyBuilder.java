/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import org.apache.ibatis.jdbc.SQL;

import com.jeesuite.mybatis.crud.CrudMethods;
import com.jeesuite.mybatis.metadata.ColumnMetadata;
import com.jeesuite.mybatis.metadata.EntityMetadata;
import com.jeesuite.mybatis.metadata.TableMetadata;

/**
 * 批量插入
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class SelectByPrimaryKeyBuilder extends AbstractSelectMethodBuilder{

	@Override
	String[] methodNames() {
		return new String[]{CrudMethods.selectByPrimaryKey.name()};
	}

	@Override
	String buildSQL(EntityMetadata entityMapper, boolean selective) {
		// 从表注解里获取表名等信息
		TableMetadata tableMapper = entityMapper.getTable();
		ColumnMetadata idColumn = entityMapper.getIdColumn();
		
		return new SQL()
		   .SELECT("*")
		   .FROM(tableMapper.getName())
		   .WHERE(idColumn.getColumn() + "=#{" + idColumn.getProperty() + "}")
		   .toString();
	}

}
