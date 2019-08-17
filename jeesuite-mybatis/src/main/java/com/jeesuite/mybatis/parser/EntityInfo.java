/**
 * 
 */
package com.jeesuite.mybatis.parser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年2月22日
 * @Copyright (c) 2015, jwww
 */
public class EntityInfo {

	private String tableName;
	
	private Class<?> entityClass;
	
	private Class<?> mapperClass;
	
	private Map<String, String> mapperSqls = new HashMap<>();
	
	private String errorMsg;
	
	private Class<?> idType;
	private String idProperty;
	
	private String idColumn;
	
	public String getErrorMsg() {
		return errorMsg;
	}

	public EntityInfo(String mapperClassName, String entityClassName) {
		try {
			if(StringUtils.isNotBlank(entityClassName))entityClass = Class.forName(entityClassName);
//			if(!BaseEntity.class.isAssignableFrom(entityClass)){
//				errorMsg = "entityClass[" + entityClassName +"] not extend[com.jeesuite.mybatis.core.BaseEntity]";
//				return;
//			}
			if(entityClass.isAnnotationPresent(Table.class)){
				this.tableName = entityClass.getAnnotation(Table.class).name();
				
				Field[] fields = entityClass.getDeclaredFields();
				for (Field field : fields) {
					if(field.isAnnotationPresent(javax.persistence.Id.class)){
						this.idType = field.getType();
						this.idProperty = field.getName();
						Column column = field.getAnnotation(javax.persistence.Column.class);
						if(column != null && StringUtils.isNotBlank(column.name())){
							this.idColumn = column.name();
						}else{
							this.idColumn = this.idProperty;
						}
						break;
					}
				}
			}else{
				errorMsg = "entityClass[" + entityClassName +"] not found annotation[@Table]";
				return;
			}
			mapperClass = Class.forName(mapperClassName);
			//
			Method[] methods = mapperClass.getDeclaredMethods();
			String sql = null;
            for (Method method : methods) {
            	sql = null;
				if(method.isAnnotationPresent(Select.class)){
					sql = method.getAnnotation(Select.class).value()[0];
				}else if(method.isAnnotationPresent(Update.class)){
					sql = method.getAnnotation(Update.class).value()[0];
				}else if(method.isAnnotationPresent(Delete.class)){
					sql = method.getAnnotation(Delete.class).value()[0];
				}
				if(sql != null){	
					String key = mapperClass.getName() + "." + method.getName();
					mapperSqls.put(key, sql);
				}
			}
		} catch (ClassNotFoundException e) {
			errorMsg = e.getMessage();
		}catch (Exception e) {
			errorMsg = String.format("parse error,please check class[{}] and [{}]", entityClassName,mapperClassName);
		}
	}
	
	public EntityInfo(String mapperClassName, String entityClassName, String tableName) {
		this.tableName = tableName;
		try {
			if(StringUtils.isNotBlank(entityClassName))entityClass = Class.forName(entityClassName);
			if(StringUtils.isBlank(this.tableName))this.tableName = entityClass.getAnnotation(Table.class).name();
			mapperClass = Class.forName(mapperClassName);
		} catch (Exception e) {
			try {					
				//根据mapper接口解析entity Class
				Type[] types = mapperClass.getGenericInterfaces();  
				Type[] tempTypes = ((ParameterizedType) types[0]).getActualTypeArguments();  
				Class<?> clazz = (Class<?>) tempTypes[0];
				if(clazz != null){
					entityClass = clazz;
				}
			} catch (Exception e1) {}
		}
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public void setEntityClass(Class<?> entityClass) {
		this.entityClass = entityClass;
	}

	public Class<?> getMapperClass() {
		return mapperClass;
	}

	public void setMapperClass(Class<?> mapperClass) {
		this.mapperClass = mapperClass;
	}
	
	public void addSql(String id,String sql){
		mapperSqls.put(mapperClass.getName() + "." + id, sql);
	}

	public Map<String, String> getMapperSqls() {
		return mapperSqls;
	}

	public Class<?> getIdType() {
		return idType;
	}

	public String getIdProperty() {
		return idProperty;
	}

	public String getIdColumn() {
		return idColumn;
	}

}
