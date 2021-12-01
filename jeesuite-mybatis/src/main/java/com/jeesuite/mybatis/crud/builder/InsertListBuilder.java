/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import java.util.Set;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.crud.SqlTemplate;
import com.jeesuite.mybatis.metadata.ColumnMetadata;
import com.jeesuite.mybatis.metadata.EntityMetadata;
import com.jeesuite.mybatis.metadata.TableMetadata;

/**
 * 批量插入
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class InsertListBuilder extends AbstractMethodBuilder{

	@Override
	SqlCommandType sqlCommandType() {
		return SqlCommandType.INSERT;
	}

	@Override
	String[] methodNames() {
		return new String[]{"insertList"};
	}

	@Override
	String buildSQL(EntityMetadata entityMapper, boolean selective) {

		// 从表注解里获取表名等信息
		TableMetadata table = entityMapper.getTable();
		Set<ColumnMetadata> columns = entityMapper.getColumns();

		StringBuilder fieldBuilder = new StringBuilder("(");
		StringBuilder prppertyBuilder = new StringBuilder("(");
		if (!entityMapper.autoId()) {
			fieldBuilder.append(entityMapper.getIdColumn().getColumn()).append(",");
			prppertyBuilder.append("#{item.").append(entityMapper.getIdColumn().getProperty()).append("},");
		}
		for (ColumnMetadata column : columns) {
			if (column.isId() || !column.isInsertable()) {
				continue;
			}
			String fieldExpr = SqlTemplate.wrapIfTag(column.getProperty(), column.getColumn(), true);
			String propertyExpr = SqlTemplate.wrapIfTag(column.getProperty(), "#{item." + column.getProperty() + "}", true);
			fieldBuilder.append(fieldExpr);
			fieldBuilder.append(",");
			prppertyBuilder.append(propertyExpr);
			prppertyBuilder.append(",");
		}
		
		fieldBuilder.deleteCharAt(fieldBuilder.length() - 1);
		prppertyBuilder.deleteCharAt(prppertyBuilder.length() - 1);
		
		fieldBuilder.append(")");
		prppertyBuilder.append(")");
		String sql = String.format(SqlTemplate.BATCH_INSERT, table.getName(),fieldBuilder.toString(),prppertyBuilder.toString());
		return sql;
	}

	@Override
	void setResultType(Configuration configuration, MappedStatement statement, Class<?> entityClass) {}
	
	@Override
	boolean scriptWrapper() {
		return true;
	}
}
