package com.jeesuite.mybatis.crud.builder;

import com.jeesuite.mybatis.crud.CrudMethods;
import com.jeesuite.mybatis.crud.SqlTemplate;
import com.jeesuite.mybatis.metadata.ColumnMetadata;
import com.jeesuite.mybatis.metadata.EntityMetadata;
import com.jeesuite.mybatis.metadata.TableMetadata;

/**
 * 
 * <br>
 * Class Name   : SelectByIdsBuilder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月9日
 */
public class SelectByPrimaryKeysBuilder extends AbstractSelectMethodBuilder{

	@Override
	String[] methodNames() {
		return new String[]{CrudMethods.selectByPrimaryKeys.name()};
	}

	@Override
	String buildSQL(EntityMetadata entityMapper, boolean selective) {
		TableMetadata tableMapper = entityMapper.getTable();
		ColumnMetadata idColumn = entityMapper.getIdColumn();
		String sql = String.format(SqlTemplate.SELECT_BY_KEYS, tableMapper.getName(),idColumn.getColumn());
		return sql;
	}
	
	@Override
	boolean scriptWrapper() {
		return true;
	}

}
