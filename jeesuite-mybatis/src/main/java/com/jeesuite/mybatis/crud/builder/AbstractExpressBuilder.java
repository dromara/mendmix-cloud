package com.jeesuite.mybatis.crud.builder;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.util.DateUtils;
import com.jeesuite.mybatis.metadata.ColumnMetadata;

/**
 * 
 * <br>
 * Class Name   : AbstractExpressBuilder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月29日
 */
public abstract class AbstractExpressBuilder {

	/**
	 * @param whereBuilder
	 * @param column
	 */
	protected StringBuilder appendWhere(StringBuilder whereBuilder, ColumnMetadata column) {
		if(whereBuilder.length() > 0)whereBuilder.append(" AND ");
		whereBuilder.append(column.getColumn()).append("=");
		whereBuilder.append("#{").append(column.getProperty()).append("}");
		return whereBuilder;
	}
	
	/**
	 * @param whereBuilder
	 * @param column
	 * @param value
	 */
	protected StringBuilder appendWhere(StringBuilder whereBuilder, ColumnMetadata column, Object value) {
		if(whereBuilder.length() > 0)whereBuilder.append(" AND ");
		whereBuilder.append(column.getColumn()).append("=");
		if(column.getJavaType() == String.class){
			whereBuilder.append("'").append(value).append("'");
		}else if(column.getJavaType() == Date.class){
			whereBuilder.append("'").append(DateUtils.format((Date)value)).append("'");
		}else if(column.getJavaType() == Boolean.class || column.getJavaType() == boolean.class){
			if(StringUtils.isNumeric(value.toString())) {
				whereBuilder.append(value);
			}else {
				whereBuilder.append((boolean)value ? 1 : 0);
			}
		}else{
			whereBuilder.append(value);
		}
		return whereBuilder;
	}
	
	
	protected StringBuilder appendUpdateSet(StringBuilder setBuilder, ColumnMetadata column) {
		if(setBuilder.length() > 0)setBuilder.append(",");
		setBuilder.append(column.getColumn()).append("=");
		setBuilder.append("#{").append(column.getProperty()).append("}");
		return setBuilder;
	}
	
	protected StringBuilder appendUpdateSet(StringBuilder setBuilder, ColumnMetadata column, Object value) {
		if(setBuilder.length() > 0)setBuilder.append(",");
		setBuilder.append(column.getColumn()).append("=");
		if(column.getJavaType() == String.class){
			setBuilder.append("'").append(value).append("'");
		}else if(column.getJavaType() == Date.class){
			setBuilder.append("'").append(DateUtils.format((Date)value)).append("'");
		}else if(column.getJavaType() == Boolean.class || column.getJavaType() == boolean.class){
			setBuilder.append((boolean)value ? 1 : 0);
		}else{
			setBuilder.append(value);
		}
		return setBuilder;
	}
}
