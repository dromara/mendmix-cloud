/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.mybatis.crud.provider;

import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.kit.MybatisMapperParser;
import org.dromara.mendmix.mybatis.metadata.ColumnMetadata;
import org.dromara.mendmix.mybatis.metadata.EntityMetadata;
import org.dromara.mendmix.mybatis.metadata.MetadataHelper;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;

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

	
	protected EntityMetadata currentEntityMetadata(Object example) {
		OnceContextVal contextVal = MybatisRuntimeContext.getOnceContextVal();
		if(contextVal != null) {
			return MybatisMapperParser.getMapperMetadata(contextVal.getMapperNameSpace()).getEntityMetadata();
		}else {
			return MetadataHelper.getEntityMapper(example.getClass());
		}
	}
	
	/**
	 * @param whereBuilder
	 * @param column
	 */
	protected void appendWhere(StringBuilder whereBuilder, ColumnMetadata column) {
		if(whereBuilder.length() > 0)whereBuilder.append(" AND ");
		if(column.isFuzzyMatch()) {
			whereBuilder.append(column.getColumn()).append(" LIKE ");
			whereBuilder.append("CONCAT('%',#{").append(column.getProperty()).append("},'%')");
		}else {
			whereBuilder.append(column.getColumn()).append("=");
			whereBuilder.append("#{").append(column.getProperty()).append("}");
		}
		
	}
	
	/**
	 * @param whereBuilder
	 * @param column
	 * @param value
	 */
	protected void appendWhere(StringBuilder whereBuilder, ColumnMetadata column, Object value) {
		if(whereBuilder.length() > 0)whereBuilder.append(" AND ");
		whereBuilder.append(column.getColumn()).append("=");
		if(column.getJavaType() == String.class){
			whereBuilder.append("'").append(value).append("'");
		}else if(column.getJavaType() == Date.class){
			whereBuilder.append("'").append(DateFormatUtils.format((Date)value,"yyyy-MM-dd HH:mm:ss")).append("'");
		}else if(column.getJavaType() == Boolean.class || column.getJavaType() == boolean.class){
			whereBuilder.append((boolean)value ? 1 : 0);
		}else{
			whereBuilder.append(value);
		}
	}
	
	
	protected void appendUpdateSet(StringBuilder setBuilder, ColumnMetadata column) {
		if(setBuilder.length() > 0)setBuilder.append(",");
		setBuilder.append(column.getColumn()).append("=");
		setBuilder.append("#{").append(column.getProperty()).append("}");
	}
	
	protected void appendUpdateSet(StringBuilder setBuilder, ColumnMetadata column, Object value) {
		if(setBuilder.length() > 0)setBuilder.append(",");
		setBuilder.append(column.getColumn()).append("=");
		if(column.getJavaType() == String.class){
			setBuilder.append("'").append(value).append("'");
		}else if(column.getJavaType() == Date.class){
			setBuilder.append("'").append(DateFormatUtils.format((Date)value,"yyyy-MM-dd HH:mm:ss")).append("'");
		}else if(column.getJavaType() == Boolean.class || column.getJavaType() == boolean.class){
			setBuilder.append((boolean)value ? 1 : 0);
		}else{
			setBuilder.append(value);
		}
	}
}
