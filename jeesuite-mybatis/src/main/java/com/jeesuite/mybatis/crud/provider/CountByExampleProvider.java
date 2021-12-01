package com.jeesuite.mybatis.crud.provider;

import java.util.Set;

import org.apache.ibatis.jdbc.SQL;

import com.jeesuite.mybatis.crud.builder.AbstractExpressBuilder;
import com.jeesuite.mybatis.metadata.ColumnMetadata;
import com.jeesuite.mybatis.metadata.EntityMetadata;
import com.jeesuite.mybatis.metadata.MetadataHelper;

/**
 * 
 * <br>
 * Class Name : CountByExampleProvider
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月11日
 */
public class CountByExampleProvider extends AbstractExpressBuilder{

	public String countByExample(Object example) throws Exception {
		EntityMetadata entityMapper = MetadataHelper.getEntityMapper(example.getClass());
		Set<ColumnMetadata> columns = entityMapper.getColumns();
		SQL sql = new SQL().SELECT("COUNT(1)").FROM(entityMapper.getTable().getName());
		Object value;
		StringBuilder whereBuilder = new StringBuilder();
		for (ColumnMetadata column : columns) {
			value = MetadataHelper.getEntityField(entityMapper.getTable().getName(),column.getProperty()).get(example);
			if(value == null)continue;
			appendWhere(whereBuilder,column);
		}
		if(whereBuilder.length() == 0)throw new IllegalArgumentException("至少包含一个查询条件");
		
		sql.WHERE(whereBuilder.toString());
		return sql.toString();
	}
}
