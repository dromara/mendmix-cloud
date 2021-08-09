/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.mybatis.crud.provider;

import java.util.Date;

import com.jeesuite.common.util.DateUtils;
import com.jeesuite.mybatis.crud.helper.ColumnMapper;

/**
 * 
 * <br>
 * Class Name   : AbstractExampleProvider
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月29日
 */
public abstract class AbstractExampleProvider {

	/**
	 * @param whereBuilder
	 * @param column
	 */
	protected void appendWhere(StringBuilder whereBuilder, ColumnMapper column) {
		if(whereBuilder.length() > 0)whereBuilder.append(" AND ");
		whereBuilder.append(column.getColumn()).append("=");
		whereBuilder.append("#{").append(column.getProperty()).append("}");
	}
	
	/**
	 * @param whereBuilder
	 * @param column
	 * @param value
	 */
	protected void appendWhere(StringBuilder whereBuilder, ColumnMapper column, Object value) {
		if(whereBuilder.length() > 0)whereBuilder.append(" AND ");
		whereBuilder.append(column.getColumn()).append("=");
		if(column.getJavaType() == String.class){
			whereBuilder.append("'").append(value).append("'");
		}else if(column.getJavaType() == Date.class){
			whereBuilder.append("'").append(DateUtils.format((Date)value)).append("'");
		}else if(column.getJavaType() == Boolean.class || column.getJavaType() == boolean.class){
			whereBuilder.append((boolean)value ? 1 : 0);
		}else{
			whereBuilder.append(value);
		}
	}
	
	
	protected void appendUpdateSet(StringBuilder setBuilder, ColumnMapper column) {
		if(setBuilder.length() > 0)setBuilder.append(",");
		setBuilder.append(column.getColumn()).append("=");
		setBuilder.append("#{").append(column.getProperty()).append("}");
	}
	
	protected void appendUpdateSet(StringBuilder setBuilder, ColumnMapper column, Object value) {
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
	}
}
