package com.jeesuite.mybatis.parser;

public class MapResultItem {

	private String tableName;
	private String entityName;
	private String propertyName;
	private String columnName;
	private String type;
	private boolean primaryKey;
	
	public MapResultItem() {}
	

	public MapResultItem(String propertyName, String columnName, String type) {
		super();
		this.propertyName = propertyName;
		this.columnName = columnName;
		this.type = type;
	}

	
	public String getTableName() {
		return tableName;
	}


	public void setTableName(String tableName) {
		this.tableName = tableName;
	}


	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName == null ? columnName : columnName.toLowerCase();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}


	public boolean isPrimaryKey() {
		return primaryKey;
	}


	public void setPrimaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
	}


	public String getEntityName() {
		return entityName;
	}


	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public boolean isNumberType(){
		return "INTEGER|SMALLINT|DECIMAL|FLOAT|TINYINT|BIGINT|NUMERIC".contains(getType().toUpperCase());
	}


	@Override
	public String toString() {
		return "MapResultItem [tableName=" + tableName + ", entityName=" + entityName + ", propertyName=" + propertyName
				+ ", columnName=" + columnName + ", type=" + type + ", primaryKey=" + primaryKey + "]";
	}
	
	
}
