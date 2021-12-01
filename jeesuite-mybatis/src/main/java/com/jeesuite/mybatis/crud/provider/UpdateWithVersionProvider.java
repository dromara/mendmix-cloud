package com.jeesuite.mybatis.crud.provider;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

import org.apache.ibatis.jdbc.SQL;

import com.jeesuite.mybatis.crud.builder.AbstractExpressBuilder;
import com.jeesuite.mybatis.metadata.ColumnMetadata;
import com.jeesuite.mybatis.metadata.MetadataHelper;
import com.jeesuite.mybatis.metadata.EntityMetadata;

/**
 * 
 * <br>
 * Class Name   : UpdateWithVersionProvider
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月11日
 */
public class UpdateWithVersionProvider extends AbstractExpressBuilder{

	public String updateByPrimaryKeyWithVersion(Object example) throws Exception {
		EntityMetadata entityMapper = MetadataHelper.getEntityMapper(example.getClass());
		Set<ColumnMetadata> columns = entityMapper.getColumns();
		SQL sql = new SQL().UPDATE(entityMapper.getTable().getName());
		
		StringBuilder setBuilder = new StringBuilder();
		StringBuilder whereBuilder = new StringBuilder();
		//主键
		ColumnMetadata idColumn = entityMapper.getIdColumn();
		appendWhere(whereBuilder,idColumn);
		
		Object value;
		for (ColumnMetadata column : columns) {
			if(column.isId() || !column.isUpdatable())continue;
			value = MetadataHelper.getEntityField(entityMapper.getTable().getName(),column.getProperty()).get(example);
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
