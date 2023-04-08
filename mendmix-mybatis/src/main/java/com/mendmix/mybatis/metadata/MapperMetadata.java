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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.mapping.SqlCommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.model.Page;
import com.mendmix.common.model.PageParams;
import com.mendmix.mybatis.exception.MybatisHanlerInitException;
import com.mendmix.mybatis.kit.MybatisSqlUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年2月22日
 * @Copyright (c) 2015, jwww
 */
public class MapperMetadata {
	
	private static final Logger log = LoggerFactory.getLogger(MapperMetadata.class);

	private static List<String> queryMethodPrefixs = Arrays.asList("select","query","get","list","find");
	
	private String group;
	
	private String tableName;
	
	private Class<?> entityClass;
	
	private Class<?> mapperClass;
	
	private EntityMetadata entityMetadata;
	
	private Map<String, List<String>> queryTableMappings = new HashMap<>();

	private Map<String, String> propToColumnMappings = new HashMap<>();

	private Map<String,MapperMethod> mapperMethods = new HashMap<>();


	public MapperMetadata(String mapperClassName) {
		try {
			mapperClass = Class.forName(mapperClassName);
			
			if(!mapperClass.isInterface())return;
			
			parseEntityInfo();
			
			if(entityClass == null)return;

			if(entityClass.isAnnotationPresent(Table.class)){
				this.tableName = entityClass.getAnnotation(Table.class).name();
			}else{
				return;
			}
			//
			List<Method> methods = new ArrayList<>();
			parseAllMethod(mapperClass, methods);
	
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
					List<String> useTables = MybatisSqlUtils.parseSqlUseTables(sql);
					queryTableMappings.put(fullName, useTables);
				}
				//
				mapperMethods.put(method.getName(),new MapperMethod(method, fullName, sqlType));
			}
            //
            List<ColumnMetadata> columns = MetadataHelper.getTableColumnMappers(tableName);
    		for (ColumnMetadata column : columns) {
    			propToColumnMappings.put(column.getProperty(), column.getColumn());
    		}
		} catch (Exception e) {
			log.info("MENDMIX-TRACE-LOGGGING-->> parse mapper error -> mapperClassName:{}",mapperClassName,e);
		}
	}
	
	public MapperMetadata(String mapperClassName, String entityClassName, String tableName) {
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

	
	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
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
	
	public void parseSqlUseTables(String id,String sql){
		String fullName = mapperClass.getName() + "." + id;
		final List<String> useTables = MybatisSqlUtils.parseSqlUseTables(sql);
		queryTableMappings.put(fullName, useTables);
	}

	public Map<String, MapperMethod> getMapperMethods() {
		return mapperMethods;
	}
	
	public MapperMethod getMapperMethod(String methodFullName) {
		return mapperMethods.get(methodFullName);
	}
	
	public EntityMetadata getEntityMetadata() {
		return entityMetadata;
	}
	
	public Map<String, List<String>> getQueryTableMappings() {
		return queryTableMappings;
	}

	public List<String> getUseTableNames(String msId){
		return queryTableMappings.get(msId);
	}
	
	public Map<String, String> getPropToColumnMappings() {
		return propToColumnMappings;
	}
	
	public String property2ColumnName(String propName) {
		return propToColumnMappings.get(propName);
	}

	private static void parseAllMethod(Class<?> clazz,List<Method> methods) {
		if(clazz.getDeclaredMethods() != null) {
			for (Method method : clazz.getDeclaredMethods()) {
				methods.add(method);
			}
		}
    	Class<?>[] interfaces = clazz.getInterfaces();
    	for (Class<?> interClass : interfaces) {
    		parseAllMethod(interClass, methods);
		}
	}
	
	private void parseEntityInfo() {
		Type[] interfacesTypes = mapperClass.getGenericInterfaces();
		if(interfacesTypes == null || interfacesTypes.length == 0)return;
		Type[] genericTypes = ((ParameterizedType) interfacesTypes[0]).getActualTypeArguments();
		if(genericTypes == null || genericTypes.length == 0)return;
		entityClass = (Class<?>) genericTypes[0];
		//
		entityMetadata = MetadataHelper.getEntityMapper(entityClass);
	}

	public static class MapperMethod {
		Method method;
		String fullName;
		SqlCommandType sqlType;
		boolean pageQuery;
		
		public MapperMethod(Method method, String fullName, SqlCommandType sqlType) {
			super();
			this.method = method;
			this.fullName = fullName;
			this.sqlType = sqlType;
			if(method.getReturnType() == Page.class){
				boolean withPageParams = false;
				Class<?>[] parameterTypes = method.getParameterTypes();
				self:for (Class<?> clazz : parameterTypes) {
					if(withPageParams = (clazz == PageParams.class || clazz.getSuperclass() == PageParams.class)){
						break self;
					}
				}
				
				if(!withPageParams){
					throw new MybatisHanlerInitException(String.format("method[%s] returnType is:Page,but not found Parameter[PageParams] in Parameters list", method.getName()));
				}
				this.pageQuery = true;
			}
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

		public boolean isPageQuery() {
			return pageQuery;
		}
		

	}

}
