package com.jeesuite.mybatis.crud.builder;

import com.jeesuite.mybatis.crud.CrudMethods;
import com.jeesuite.mybatis.crud.SqlTemplate;
import com.jeesuite.mybatis.crud.helper.ColumnMapper;
import com.jeesuite.mybatis.crud.helper.EntityMapper;
import com.jeesuite.mybatis.crud.helper.TableMapper;

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
	String buildSQL(EntityMapper entityMapper, boolean selective) {
		TableMapper tableMapper = entityMapper.getTableMapper();
		ColumnMapper idColumn = entityMapper.getIdColumn();
		String sql = String.format(SqlTemplate.SELECT_BY_KEYS, tableMapper.getName(),idColumn.getColumn());
		return String.format(SqlTemplate.SCRIPT_TEMAPLATE, sql);
	}

}
