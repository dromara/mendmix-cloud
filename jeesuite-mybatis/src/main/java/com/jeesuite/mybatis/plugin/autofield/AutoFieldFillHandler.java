package com.jeesuite.mybatis.plugin.autofield;

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

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.guid.GUID;
import com.jeesuite.mybatis.MybatisConfigs;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.metadata.MapperMetadata;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.InvocationVals;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;
import com.jeesuite.mybatis.plugin.autofield.annotation.CreatedAt;
import com.jeesuite.mybatis.plugin.autofield.annotation.CreatedBy;
import com.jeesuite.mybatis.plugin.autofield.annotation.UpdatedAt;
import com.jeesuite.mybatis.plugin.autofield.annotation.UpdatedBy;
import com.jeesuite.spring.InstanceFactory;

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
			if(anyNotNull(createdFields)) {
	        	methodFieldMappings.put(keyPrefix + "insert", createdFields);
	        	methodFieldMappings.put(keyPrefix + "insertSelective", createdFields);
	        	methodFieldMappings.put(keyPrefix + INSERT_LIST_METHOD_NAME, createdFields);
	        }
			if(anyNotNull(updatedFields)) {
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
		if(methodFieldMappings.isEmpty())return null;
		
        final MappedStatement orignMappedStatement = invocation.getMappedStatement();
        
        Field[] fields = methodFieldMappings.get(orignMappedStatement.getId());
		if(fields == null) return null;
		
		Object parameter = invocation.getParameter();
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
	}
	
	private boolean anyNotNull(Field[] fields) {
		for (Field field : fields) {
			if(field != null)return true;
		}
		return false;
	}
	
	private boolean isNullValue(Object obj,Field field) {
		try {
			Object value = field.get(field);
			return value == null || StringUtils.isBlank(value.toString());
		} catch (Exception e) {
			return true;
		}
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
