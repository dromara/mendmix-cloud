/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import java.util.Set;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.crud.SqlTemplate;
import com.jeesuite.mybatis.crud.helper.ColumnMapper;
import com.jeesuite.mybatis.crud.helper.EntityMapper;
import com.jeesuite.mybatis.crud.helper.TableMapper;

/**
 * 批量插入
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class UpdateBuilder  extends AbstractMethodBuilder{

	@Override
	SqlCommandType sqlCommandType() {
		return SqlCommandType.UPDATE;
	}

	@Override
	String[] methodNames() {
		return new String[]{"updateByPrimaryKey","updateByPrimaryKeySelective"};
	}

	@Override
	String buildSQL(EntityMapper entityMapper, boolean selective) {

		// 从表注解里获取表名等信息
		TableMapper tableMapper = entityMapper.getTableMapper();
		Set<ColumnMapper> columnMappers = entityMapper.getColumnsMapper();
		
		String idColumn = null;
		String idProperty = null;
		StringBuilder set = new StringBuilder();
		set.append("<trim prefix=\"SET\" suffixOverrides=\",\">");
		for (ColumnMapper column : columnMappers) {
			if (!column.isUpdatable()) {
				continue;
			}
			if (column.isId()) {
				idColumn= column.getColumn();
				idProperty = column.getProperty();
			}else{
				String expr = SqlTemplate.wrapIfTag(column.getProperty(), column.getColumn() +"=#{"+column.getProperty()+"}", !selective);
				set.append(expr);
				if(!selective)set.append(",");
			}
		}
		if(!selective)set.deleteCharAt(set.length() - 1);
		set.append("</trim>");

		String sql = String.format(SqlTemplate.UPDATE_BY_KEY, tableMapper.getName(),set.toString(),idColumn,idProperty);

		return String.format(SqlTemplate.SCRIPT_TEMAPLATE, sql);
	}


	@Override
	void setResultType(Configuration configuration, MappedStatement statement, Class<?> entityClass) {}
	
}

