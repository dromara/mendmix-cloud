/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.common.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.util.StringConverter;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年6月14日
 */
public class Condition {

	private static final String IS_NOT_NULL = "isNotNull";
	private static final String IS_NULL = "isNull";

	private static final List<String> OPERATORS = Arrays.asList("<>", "<=", "<", ">=", ">", "=", "IN",IS_NULL,IS_NOT_NULL);

	private String operator;
	private String column;
	private String value;

	public Condition() {}
	
	public Condition(String column, String operator, String value) {
		this.column = column;
		this.operator = operator;
		this.value = value;
	}



	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	
	public String getColumn() {
		return column;
	}
	public void setColumn(String column) {
		this.column = column;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	public static JdbcExecParam buildJdbcExecObject(String select,List<Condition> conditions,boolean camelToUnderscoreCase) {
		if(conditions == null || conditions.isEmpty()) {
			throw new MendmixBaseException("条件不能为空");
		}
		
		StringBuilder builder = new StringBuilder(select);
		List<Object> params = new ArrayList<>();
		
		boolean withWhere = select.toUpperCase().contains("WHERE");
		if(!withWhere) {
			builder.append(StringUtils.SPACE).append("WHERE");
		}else {
			builder.append(StringUtils.SPACE).append("AND");
		}
		
		String column;
		Condition condition;
		for (int i = 0; i < conditions.size(); i++) {
			condition = conditions.get(i);
			if(!OPERATORS.contains(condition.getOperator())) {
				throw new MendmixBaseException("不支持操作符:" + condition.getOperator());
			}
			column = camelToUnderscoreCase ? StringConverter.toUnderlineCase(condition.getColumn()) : condition.getColumn();
			
			if(i > 0)builder.append(StringUtils.SPACE).append("AND");
			builder.append(StringUtils.SPACE).append(column);
			if(IS_NULL.equals(condition.getOperator())) {
				builder.append(" IS NULL");
			}else if(IS_NOT_NULL.equals(condition.getOperator())) {
				builder.append(" IS NOT NULL");
			}else {
				builder.append(condition.getOperator()).append("?");
				params.add(condition.getValue());
			}
		}
		
		return new JdbcExecParam(builder.toString(), params.toArray());
	}
}
