package com.jeesuite.mybatis.crud.provider;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

import org.apache.ibatis.jdbc.SQL;

import com.jeesuite.mybatis.crud.helper.ColumnMapper;
import com.jeesuite.mybatis.crud.helper.EntityHelper;
import com.jeesuite.mybatis.crud.helper.EntityMapper;

/**
 * 
 * <br>
 * Class Name   : UpdateWithVersionProvider
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月11日
 */
public class UpdateWithVersionProvider extends AbstractExampleProvider{

	public String updateByPrimaryKeyWithVersion(Object example) throws Exception {
		EntityMapper entityMapper = EntityHelper.getEntityMapper(example.getClass());
		Set<ColumnMapper> columns = entityMapper.getColumnsMapper();
		SQL sql = new SQL().UPDATE(entityMapper.getTableMapper().getName());
		
		StringBuilder setBuilder = new StringBuilder();
		StringBuilder whereBuilder = new StringBuilder();
		//主键
		ColumnMapper idColumn = entityMapper.getIdColumn();
		appendWhere(whereBuilder,idColumn);
		
		Object value;
		for (ColumnMapper column : columns) {
			if(column.isId() || !column.isUpdatable())continue;
			value = EntityHelper.getEntityField(entityMapper.getTableMapper().getName(),column.getProperty()).get(example);
			if(value == null)continue;
			if(column.isVersionField()) {
				appendWhere(whereBuilder,column);
				//乐观锁新值
				value = getNewVersionVal(column.getJavaType(), value);
				appendUpdateSet(setBuilder, column,value);
			}else {
				appendUpdateSet(setBuilder, column);
			}
		}
		sql.SET(setBuilder.toString());
		sql.WHERE(whereBuilder.toString());
		return sql.toString();
	}

	private Object getNewVersionVal(Class<?> clazz, Object originalVersionVal) {
        if (long.class.equals(clazz) || Long.class.equals(clazz)) {
            return ((long) originalVersionVal) + 1;
        } else if (int.class.equals(clazz) || Integer.class.equals(clazz)) {
            return ((int) originalVersionVal) + 1;
        } else if (Date.class.equals(clazz)) {
            return new Date();
        } else if (Timestamp.class.equals(clazz)) {
            return new Timestamp(System.currentTimeMillis());
        } else if (LocalDateTime.class.equals(clazz)) {
            return LocalDateTime.now();
        }
        return originalVersionVal;
    }
}
