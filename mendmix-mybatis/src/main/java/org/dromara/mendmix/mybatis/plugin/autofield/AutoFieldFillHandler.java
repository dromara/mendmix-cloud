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
package org.dromara.mendmix.mybatis.plugin.autofield;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.util.CachingFieldUtils;
import org.dromara.mendmix.common.util.ExceptionFormatUtils;
import org.dromara.mendmix.mybatis.MybatisConfigs;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.core.BaseEntity;
import org.dromara.mendmix.mybatis.crud.CrudMethods;
import org.dromara.mendmix.mybatis.kit.MybatisMapperParser;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.dromara.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.PluginInterceptorHandler;
import org.dromara.mendmix.mybatis.plugin.autofield.annotation.CreatedAt;
import org.dromara.mendmix.mybatis.plugin.autofield.annotation.CreatedBy;
import org.dromara.mendmix.mybatis.plugin.autofield.annotation.UpdatedAt;
import org.dromara.mendmix.mybatis.plugin.autofield.annotation.UpdatedBy;
import org.dromara.mendmix.spring.InstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 字段自动填充
 * 
 * <br>
 * Class Name   : AutoFieldFillHandler
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2020年08月11日
 */
public class AutoFieldFillHandler implements PluginInterceptorHandler {


	protected static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");
	
	private static Map<String, Field[]> methodFieldMappings = new HashMap<>();
	
	private IDGenerator idGenerator;

	public IDGenerator idGenerator() {
		if(idGenerator != null)return idGenerator;
		synchronized (AutoFieldFillHandler.class) {
			idGenerator = InstanceFactory.getInstance(IDGenerator.class);
			if(idGenerator == null)idGenerator = new DefaultIdGenerator();
		}
		return idGenerator;
	}

	@Override
	public void start(MendmixMybatisInterceptor context) {
		List<MapperMetadata> entityInfos = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		
		String tenantColumn = MybatisConfigs.getTenantColumnName(context.getGroupName());
		String mapperPackageName;
		for (MapperMetadata ei : entityInfos) {
			mapperPackageName = ei.getMapperClass().getName();
			Field[] createdFields = new Field[4];
			Field[] updatedFields = new Field[4];
			Field[] fields = FieldUtils.getAllFields(ei.getEntityClass());
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
				}else if(tenantColumn != null && !MybatisConfigs.ignoreTenant(mapperPackageName)) {
					Optional<Entry<String, String>> optional = ei.getPropToColumnMappings().entrySet().stream().filter(e -> {
						return e.getValue().equalsIgnoreCase(tenantColumn);
					}).findFirst();
					//
					if(optional.isPresent() && field.getName().equals(optional.get().getKey())) {
						field.setAccessible(true);
						createdFields[3] = field;
					}
				}
			}
	
			String keyPrefix = ei.getMapperClass().getName() + ".";
			if(hasAnyValue(createdFields)) {
	        	methodFieldMappings.put(keyPrefix + CrudMethods.insert.name(), createdFields);
	        	methodFieldMappings.put(keyPrefix + CrudMethods.insertSelective.name(), createdFields);
	        	methodFieldMappings.put(keyPrefix + CrudMethods.insertList.name(), createdFields);
	        }
			if(hasAnyValue(updatedFields)) {
	        	methodFieldMappings.put(keyPrefix + CrudMethods.updateByPrimaryKey.name(), updatedFields);
	        	methodFieldMappings.put(keyPrefix + CrudMethods.updateByPrimaryKeySelective.name(), updatedFields);
	        	methodFieldMappings.put(keyPrefix + CrudMethods.updateByPrimaryKeyWithVersion.name(), updatedFields);
	        	methodFieldMappings.put(keyPrefix + CrudMethods.updateListByPrimaryKeys.name(), updatedFields);
	        	methodFieldMappings.put(keyPrefix + CrudMethods.updateListByPrimaryKeysSelective.name(), updatedFields);
	        }
		}
        
	}


	@SuppressWarnings("unchecked")
	@Override
	public Object onInterceptor(OnceContextVal invocation) throws Throwable {
		if(invocation.getMappedStatement().getSqlCommandType() == SqlCommandType.SELECT 
				|| invocation.getMappedStatement().getSqlCommandType() == SqlCommandType.DELETE) {
			return null;
		}
		Object parameter = invocation.getParameter();
		//动态拓展字段
		boolean dynaFieldEnabled = ThreadLocalContext.exists(MybatisRuntimeContext.PARAM_CONTEXT_NAME);
		
		if(!dynaFieldEnabled && methodFieldMappings.isEmpty())return null;
		
        final MappedStatement mt = invocation.getMappedStatement();
		
		Field[] fields = methodFieldMappings.get(mt.getId());
		if(fields != null) {
			boolean updateListByPrimaryKeys = mt.getId().endsWith(CrudMethods.updateListByPrimaryKeys.name());
			boolean updateAction = updateListByPrimaryKeys || SqlCommandType.UPDATE.equals(mt.getSqlCommandType());
			if(updateListByPrimaryKeys 
					|| mt.getId().endsWith(CrudMethods.insertList.name()) 
					|| mt.getId().endsWith(CrudMethods.updateListByPrimaryKeysSelective.name())
			  ) {
				if(parameter instanceof Map) {
					try {
						List<Object> list = (List<Object>) ((Map<String, Object>)parameter).get("arg0");
						for (Object obj : list) {
							setFieldValues(fields,obj,updateAction);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}else {
				setFieldValues(fields,parameter,updateAction);
			}
		}
		
		return null;
	}


	private void setFieldValues(Field[] fields, Object parameter,boolean updateAction) {
		String tmpVal;
		
		if(fields[0] != null && idGenerator() != null && isNullValue(parameter, fields[0])) {
			Object id = idGenerator.next();
			if(fields[0].getType() == Long.class) {
				id = Long.parseLong(id.toString());
			}
			setFieldValue(parameter, fields[0], id);
		}
		
		if(fields[1] != null && (tmpVal = MybatisRuntimeContext.getCurrentUserId()) != null && (updateAction || isNullValue(parameter, fields[1]))) {
			setFieldValue(parameter, fields[1], tmpVal);
		}

		if(fields[2] != null && (updateAction || isNullValue(parameter, fields[2]))) {
			setFieldValue(parameter, fields[2], new Date());
		}
		
		if(fields[3] != null && (tmpVal = MybatisRuntimeContext.getCurrentTenant()) != null) {
			if(!MybatisRuntimeContext.isIgnoreTenantMode() || isNullValue(parameter, fields[3])) {
				setFieldValue(parameter, fields[3], tmpVal);
			}
		}
		//
		setDynaFieldValues(parameter);
	}
	
	private boolean isNullValue(Object obj,Field field) {
		try {
			Object value = field.get(obj);
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
	
	private void setDynaFieldValues(Object entity) {
		Map<String, String> dynaValueMap = tryGetDynaAttrValues(entity);
		boolean debugMode = CurrentRuntimeContext.isDebugMode();
		if(debugMode) {
			logger.info("{} dynaValueMap:{}",ExceptionFormatUtils.buildLogHeader("DynaFormHandle", "debugMode"),dynaValueMap);
		}
		if(dynaValueMap != null) {
			dynaValueMap.forEach( (k,v) -> {
				CachingFieldUtils.writeField(entity, k, v);
				if(debugMode) {
					logger.info("{} write {} = {}",ExceptionFormatUtils.buildLogHeader("DynaFormHandle", "debugMode"),k,v);
				}
			} );
		}
	}
	
	private Map<String, String> tryGetDynaAttrValues(Object parameter){
		
		if(parameter instanceof BaseEntity == false)return null;
		
		boolean debugMode = CurrentRuntimeContext.isDebugMode();
		
		Class<?> entityClass = parameter.getClass();
		if(!ThreadLocalContext.exists(MybatisRuntimeContext.PARAM_CONTEXT_NAME))return null;

		String key;
		Map<String, String> dynaValueMap;
		Object id = CachingFieldUtils.readField(parameter, BaseEntity.DEFAULT_ID_FIELD);
		if(id != null) {
			if(debugMode) {
				logger.info("{} id:{}",ExceptionFormatUtils.buildLogHeader("DynaFormHandle", "debugMode"),id);
			}
			key = String.format(MybatisRuntimeContext.ATTR_VALUE_CONTEXT_NAME, entityClass.getSimpleName(),id);
			dynaValueMap = ThreadLocalContext.get(key);
			if(dynaValueMap != null) {
				if(logger.isDebugEnabled()) {
					logger.debug("<framework-logging> mybatisAutoFieldHandle get_context_dynaValue -> key:{},values:{}",key,dynaValueMap);
				}
				ThreadLocalContext.remove(key);
				return dynaValueMap;
			}
		}
		
		String listCountKey = String.format(MybatisRuntimeContext.DYNA_LIST_COUNT_CONTEXT_NAME, entityClass.getSimpleName());
		Integer listCount = ThreadLocalContext.get(listCountKey, 0);
		for (int i = 0; i <= listCount; i++) {
			key = String.format(MybatisRuntimeContext.ATTR_VALUE_CONTEXT_NAME, entityClass.getSimpleName(),i);
			dynaValueMap = ThreadLocalContext.get(key);
			if(dynaValueMap != null) {
				if(logger.isDebugEnabled()) {
					logger.debug("<framework-logging> mybatisAutoFieldHandle get_context_dynaValue -> key:{},values:{}",key,dynaValueMap);
				}
				ThreadLocalContext.remove(key);
				return dynaValueMap;
			}
		}
		return null;
	}

	@Override
	public void onFinished(OnceContextVal invocationVal, Object result) {}
	
	@Override
	public void close() {}

	@Override
	public int interceptorOrder() {
		return 1;
	}

}
