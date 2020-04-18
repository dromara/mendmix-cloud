/**
 * 
 */
package com.jeesuite.mybatis.parser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.mapping.SqlCommandType;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年2月22日
 * @Copyright (c) 2015, jwww
 */
public class EntityInfo {

	private static List<String> queryMethodPrefixs = Arrays.asList("select","query","get","list","find");
	private String tableName;
	
	private Class<?> entityClass;
	
	private Class<?> mapperClass;
	
	private Map<String, String> mapperSqls = new HashMap<>();
	
	private String errorMsg;
	
	private Class<?> idType;
	private String idProperty;
	
	private String idColumn;
	
	private List<MapperMethod> mapperMethods = new ArrayList<>();
	
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
			List<Method> methods = new ArrayList<>(Arrays.asList(mapperClass.getDeclaredMethods()));
			Class<?>[] interfaces = mapperClass.getInterfaces();
			if(interfaces != null){		
				for (Class<?> superClass : interfaces) {					
					methods.addAll(Arrays.asList(superClass.getDeclaredMethods()));
				}
			}
			
			String sql = null;
			String fullName;
			SqlCommandType sqlType;
            for (Method method : methods) {
            	fullName = mapperClass.getName() + "." + method.getName();
				if(method.isAnnotationPresent(Select.class)){
					sql = method.getAnnotation(Select.class).value()[0];
					sqlType = SqlCommandType.SELECT;
				}else if(method.isAnnotationPresent(Update.class)){
					sql = method.getAnnotation(Update.class).value()[0];
					sqlType = SqlCommandType.UPDATE;
				}else if(method.isAnnotationPresent(Delete.class)){
					sql = method.getAnnotation(Delete.class).value()[0];
					sqlType = SqlCommandType.DELETE;
				}else if(method.isAnnotationPresent(Insert.class)){
					sql = method.getAnnotation(Insert.class).value()[0];
					sqlType = SqlCommandType.INSERT;
				}else{
					if(queryMethodPrefixs.stream().anyMatch(e -> method.getName().startsWith(e))){
						sqlType = SqlCommandType.SELECT;
					}else{
						sqlType = null;
					}
					sql = null;
				}
				
				if(sql != null){	
					mapperSqls.put(fullName, sql);
				}
				//
				mapperMethods.add(new MapperMethod(method, fullName, sqlType));
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
	
	public void addSql(String method,String id,String sql){
		String fullName = mapperClass.getName() + "." + id;
		mapperSqls.put(fullName, sql);
		for (MapperMethod mapperMethod : mapperMethods) {
			if(mapperMethod.getFullName().equals(fullName)){
				mapperMethod.sqlType = SqlCommandType.valueOf(method.toUpperCase());
				break;
			}
		}
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
	
	public List<MapperMethod> getMapperMethods() {
		return mapperMethods;
	}

	public static class MapperMethod {
		Method method;
		String fullName;
		SqlCommandType sqlType;
		
		public MapperMethod(Method method, String fullName, SqlCommandType sqlType) {
			super();
			this.method = method;
			this.fullName = fullName;
			this.sqlType = sqlType;
		}
		
		public Method getMethod() {
			return method;
		}
		public String getFullName() {
			return fullName;
		}
		public SqlCommandType getSqlType() {
			return sqlType;
		}

	}

}
