package com.jeesuite.mybatis.crud.helper;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

public class ColumnMapper {

	private String property;
	private String column;
	private Class<?> javaType;
	private JdbcType jdbcType;
	private Class<? extends TypeHandler<?>> typeHandler;
	private boolean id = false;
	private boolean insertable = true;
	private boolean updatable = true;
	private boolean versionField = false;
	private boolean createdByField = false;
	private boolean createdAtField = false;
	private boolean updatedByField = false;
	private boolean updatedAtField = false;

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	public Class<?> getJavaType() {
		return javaType;
	}

	public void setJavaType(Class<?> javaType) {
		this.javaType = javaType;
	}

	public boolean isId() {
		return id;
	}

	public void setId(boolean id) {
		this.id = id;
	}

	public boolean isInsertable() {
		return insertable;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public boolean isUpdatable() {
		return updatable;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	public JdbcType getJdbcType() {
		return jdbcType;
	}

	public void setJdbcType(JdbcType jdbcType) {
		this.jdbcType = jdbcType;
	}

	public Class<? extends TypeHandler<?>> getTypeHandler() {
		return typeHandler;
	}

	public void setTypeHandler(Class<? extends TypeHandler<?>> typeHandler) {
		this.typeHandler = typeHandler;
	}
	

	public boolean isVersionField() {
		return versionField;
	}

	public void setVersionField(boolean versionField) {
		this.versionField = versionField;
	}
	

	public boolean isCreatedByField() {
		return createdByField;
	}

	public void setCreatedByField(boolean createdByField) {
		this.createdByField = createdByField;
	}

	public boolean isCreatedAtField() {
		return createdAtField;
	}

	public void setCreatedAtField(boolean createdAtField) {
		this.createdAtField = createdAtField;
	}

	public boolean isUpdatedByField() {
		return updatedByField;
	}

	public void setUpdatedByField(boolean updatedByField) {
		this.updatedByField = updatedByField;
	}

	public boolean isUpdatedAtField() {
		return updatedAtField;
	}

	public void setUpdatedAtField(boolean updatedAtField) {
		this.updatedAtField = updatedAtField;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((column == null) ? 0 : column.hashCode());
		result = prime * result + ((property == null) ? 0 : property.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColumnMapper other = (ColumnMapper) obj;
		if (column == null) {
			if (other.column != null)
				return false;
		} else if (!column.equals(other.column))
			return false;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ColumnMapper [property=" + property + ", column=" + column + "]";
	}

}
