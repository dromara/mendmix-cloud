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
package com.mendmix.mybatis.metadata;

import java.util.ArrayList;
import java.util.List;

public class SqlMetadata {
	String sql;
	String tableName;
	int whereParameterStartIndex;
	int whereParameterEndIndex;
	List<Object> parameters;
	
	
	public SqlMetadata(String sql, String tableName, int whereParameterStartIndex, int whereParameterEndIndex) {
		super();
		this.sql = sql;
		this.tableName = tableName;
		this.whereParameterStartIndex = whereParameterStartIndex;
		this.whereParameterEndIndex = whereParameterEndIndex;
		parameters = new ArrayList<>(whereParameterEndIndex - whereParameterStartIndex);
	}
	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public int getWhereParameterStartIndex() {
		return whereParameterStartIndex;
	}
	public void setWhereParameterStartIndex(int whereParameterStartIndex) {
		this.whereParameterStartIndex = whereParameterStartIndex;
	}
	public int getWhereParameterEndIndex() {
		return whereParameterEndIndex;
	}
	public void setWhereParameterEndIndex(int whereParameterEndIndex) {
		this.whereParameterEndIndex = whereParameterEndIndex;
	}
	public List<Object> getParameters() {
		return parameters;
	}
	public void setParameters(List<Object> parameters) {
		this.parameters = parameters;
	}

	
}
