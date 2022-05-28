/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.mybatis.crud.builder;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.util.DateUtils;
import com.mendmix.mybatis.metadata.ColumnMetadata;

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
