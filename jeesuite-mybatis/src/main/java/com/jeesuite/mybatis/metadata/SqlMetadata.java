package com.jeesuite.mybatis.metadata;

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
