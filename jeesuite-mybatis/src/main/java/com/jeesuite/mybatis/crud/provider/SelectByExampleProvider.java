package com.jeesuite.mybatis.crud.provider;

import java.util.Set;

import org.apache.ibatis.jdbc.SQL;

import com.jeesuite.mybatis.crud.helper.ColumnMapper;
import com.jeesuite.mybatis.crud.helper.EntityHelper;
import com.jeesuite.mybatis.crud.helper.EntityMapper;

/**
 * 
 * <br>
 * Class Name   : CountByExampleProvider
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月11日
 */
public class SelectByExampleProvider extends AbstractExampleProvider{

	public String selectByExample(Object example) throws Exception {
		EntityMapper entityMapper = EntityHelper.getEntityMapper(example.getClass());
		Set<ColumnMapper> columns = entityMapper.getColumnsMapper();
		SQL sql = new SQL().SELECT("*").FROM(entityMapper.getTableMapper().getName());
		Object value;
		StringBuilder whereBuilder = new StringBuilder();
		for (ColumnMapper column : columns) {
			value = EntityHelper.getEntityField(entityMapper.getTableMapper().getName(),column.getProperty()).get(example);
			if(value == null)continue;
			appendWhere(whereBuilder,column,value);
		}
		if(whereBuilder.length() == 0)throw new IllegalArgumentException("至少包含一个查询条件");
		//
//		if(DbType.MYSQL.name().equalsIgnoreCase(MybatisConfigs.getDbType("default"))){
//			whereBuilder.append(" LIMIT 20000");
//		}
		sql.WHERE(whereBuilder.toString());
		return sql.toString();
	}

	
}
