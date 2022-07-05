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
package com.mendmix.mybatis.plugin.autofield;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.mapping.MappedStatement;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.guid.GUID;
import com.mendmix.mybatis.MybatisConfigs;
import com.mendmix.mybatis.core.BaseEntity;
import com.mendmix.mybatis.core.InterceptorHandler;
import com.mendmix.mybatis.metadata.MapperMetadata;
import com.mendmix.mybatis.parser.MybatisMapperParser;
import com.mendmix.mybatis.plugin.InvocationVals;
import com.mendmix.mybatis.plugin.JeesuiteMybatisInterceptor;
import com.mendmix.mybatis.plugin.autofield.annotation.CreatedAt;
import com.mendmix.mybatis.plugin.autofield.annotation.CreatedBy;
import com.mendmix.mybatis.plugin.autofield.annotation.UpdatedAt;
import com.mendmix.mybatis.plugin.autofield.annotation.UpdatedBy;
import com.mendmix.spring.InstanceFactory;

/**
 * 字段自动填充
 * 
 * <br>
 * Class Name   : AutoFieldFillHandler
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Aug 8, 2021
 */
public class AutoFieldFillHandler implements InterceptorHandler {

	private static final String INSERT_LIST_METHOD_NAME = "insertList";
	private static final String ATTR_CONTEXT_NAME = "__attr_cxt_name";
	private static final String ATTR_VALUE_CONTEXT_NAME = "__attrval_cxt_name:%s:%s";

	private static Map<String, Field[]> methodFieldMappings = new HashMap<>();
	
	private  IDGenerator idGenerator;
	
	private IDGenerator getIdGenerator() {
		if(idGenerator != null)return idGenerator;
		synchronized (AutoFieldFillHandler.class) {
			idGenerator = InstanceFactory.getInstance(IDGenerator.class);
			if(idGenerator == null) {
				idGenerator = new IDGenerator() {
					@Override
					public Serializable nextId() {
						return String.valueOf(GUID.guid());
					}
				};
			}
		}
		return idGenerator;
	}


	@Override
	public void start(JeesuiteMybatisInterceptor context) {
		List<MapperMetadata> mappers = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		
		String tenantSharddingField = MybatisConfigs.getTenantSharddingField(context.getGroupName());
		for (MapperMetadata mm : mappers) {
			Field[] createdFields = new Field[4];
			Field[] updatedFields = new Field[3];
			Field[] fields = FieldUtils.getAllFields(mm.getEntityClass());
			for (Field field : fields) {
				if(field.isAnnotationPresent(Id.class) && !field.isAnnotationPresent(GeneratedValue.class)) {
					field.setAccessible(true);
					createdFields[0] = field;
				}else if(field.isAnnotationPresent(CreatedBy.class)) {
					field.setAccessible(true);
					createdFields[1] = field;
				}else if(field.isAnnotationPresent(CreatedAt.class)) {
					field.setAccessible(true);
					createdFields[2] = field;
				}else if(field.isAnnotationPresent(UpdatedBy.class)) {
					field.setAccessible(true);
					updatedFields[1] = field;
				}else if(field.isAnnotationPresent(UpdatedAt.class)) {
					field.setAccessible(true);
					updatedFields[2] = field;
				}else if(tenantSharddingField != null && field.getName().endsWith(tenantSharddingField)) {
					field.setAccessible(true);
					createdFields[3] = field;
				}
			}
	
			String keyPrefix = mm.getMapperClass().getName() + ".";
			if(hasAnyValue(createdFields)) {
	        	methodFieldMappings.put(keyPrefix + "insert", createdFields);
	        	methodFieldMappings.put(keyPrefix + "insertSelective", createdFields);
	        	methodFieldMappings.put(keyPrefix + INSERT_LIST_METHOD_NAME, createdFields);
	        }
			if(hasAnyValue(updatedFields)) {
	        	methodFieldMappings.put(keyPrefix + "updateByPrimaryKey", updatedFields);
	        	methodFieldMappings.put(keyPrefix + "updateByPrimaryKeySelective", updatedFields);
	        	methodFieldMappings.put(keyPrefix + "updateByPrimaryKeyWithVersion", updatedFields);
	        }
		}
        
	}


	@SuppressWarnings("unchecked")
	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
		if(invocation.isSelect())return null;
		Object parameter = invocation.getParameter();
		//动态拓展字段
		boolean dynaFieldEnabled = ThreadLocalContext.exists(ATTR_CONTEXT_NAME);
		
		if(!dynaFieldEnabled && methodFieldMappings.isEmpty())return null;
		
        final MappedStatement orignMappedStatement = invocation.getMappedStatement();
        
        Field[] fields = methodFieldMappings.get(orignMappedStatement.getId());
		if(fields == null) return null;

		if(orignMappedStatement.getId().endsWith(INSERT_LIST_METHOD_NAME)) {
			if(parameter instanceof Map) {
				try {
					List<Object> list = (List<Object>) ((Map<String, Object>)parameter).get("arg0");
					for (Object obj : list) {
						setFieldValue(fields,obj);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}else {
			setFieldValue(fields,parameter);
		}
		
		return null;
	}

	

	private void setFieldValue(Field[] fields, Object parameter) {
		String tmpVal;
		if(fields[0] != null && getIdGenerator() != null && isNullValue(parameter, fields[0])) {
			Serializable id = idGenerator.nextId();
//			if(fields[0].getType() == int.class || fields[0].getType() == Integer.class){
//				id = Integer.parseInt(id.toString());
//			}else if(fields[0].getType() == long.class || fields[0].getType() == Long.class){
//				id = Long.parseLong(id.toString());
//			}
			try {fields[0].set(parameter, id);} catch (Exception e) {}
		}
		
		if(fields[1] != null && (tmpVal = CurrentRuntimeContext.getCurrentUserId()) != null && isNullValue(parameter, fields[1])) {
			try {fields[1].set(parameter, tmpVal);} catch (Exception e) {}
		}
		
		if(fields[2] != null && isNullValue(parameter, fields[2])) {
			try {fields[2].set(parameter, new Date());} catch (Exception e) {}
		}
		
		if(fields.length > 3 && fields[3] != null && (tmpVal = CurrentRuntimeContext.getTenantId()) != null && isNullValue(parameter, fields[3])) {
			try {fields[3].set(parameter, tmpVal);} catch (Exception e) {}
		}
		
		//
		setDynaFieldValues(parameter);
	}

	
	private boolean isNullValue(Object obj,Field field) {
		try {
			Object value = field.get(field);
			return value == null || StringUtils.isBlank(value.toString());
		} catch (Exception e) {
			return true;
		}
	}
	
	
	private boolean hasAnyValue(Field[] fields) {
		for (Field field : fields) {
			if(field != null)return true;
		}
		return false;
	}
	
	public void setFieldValue(Object target ,Field field,Object value) {
		try {
			field.set(target, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Object readFieldValue(final Object target, final String fieldName) {
		Field field;
		try {
			Class<?> clazz = target.getClass();
			field = FieldUtils.getDeclaredField(clazz, fieldName, true);
			while(field == null && clazz.getSuperclass() != null) {
				clazz = clazz.getSuperclass();
				field = FieldUtils.getDeclaredField(clazz, fieldName, true);
			}
			if(field == null)return null;
			return field.get(target);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private void setDynaFieldValues(Object entity) {
		Map<String, String> dynaValueMap = tryGetDynaAttrValues(entity);
		if(dynaValueMap != null) {
			dynaValueMap.forEach( (k,v) -> {
				try {
					FieldUtils.writeDeclaredField(entity, k, v, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} );
		}
	}
	
	private Map<String, String> tryGetDynaAttrValues(Object parameter){
		
		if(parameter instanceof BaseEntity == false)return null;
		
		Class<?> entityClass = parameter.getClass();
		if(!ThreadLocalContext.exists(ATTR_CONTEXT_NAME))return null;
		
		String key;
		Map<String, String> dynaValueMap;
		//
		Object id = readFieldValue(parameter, "id");
		if(id != null) {
			key = String.format(ATTR_VALUE_CONTEXT_NAME, entityClass.getSimpleName(),id);
			dynaValueMap = ThreadLocalContext.get(key);
			if(dynaValueMap != null) {
				ThreadLocalContext.remove(key);
				return dynaValueMap;
			}
		}
		
		for (int i = 0; i < 10; i++) {
			key = String.format(ATTR_VALUE_CONTEXT_NAME, entityClass.getSimpleName(),i);
			dynaValueMap = ThreadLocalContext.get(key);
			if(dynaValueMap != null) {
				ThreadLocalContext.remove(key);
				return dynaValueMap;
			}
		}
		return null;
	}

	@Override
	public void onFinished(InvocationVals invocationVal, Object result) {}
	
	@Override
	public void close() {}

	@Override
	public int interceptorOrder() {
		return 1;
	}

}