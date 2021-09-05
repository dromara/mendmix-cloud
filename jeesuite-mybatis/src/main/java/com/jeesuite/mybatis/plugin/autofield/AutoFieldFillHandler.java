package com.jeesuite.mybatis.plugin.autofield;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.mapping.MappedStatement;

import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.InvocationVals;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;
import com.jeesuite.mybatis.plugin.autofield.annotation.CreatedAt;
import com.jeesuite.mybatis.plugin.autofield.annotation.CreatedBy;
import com.jeesuite.mybatis.plugin.autofield.annotation.TenantShardding;
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

	public static final String NAME = "autoField";
	
	private static final String INSERT_LIST_METHOD_NAME = "insertList";

	private static Map<String, Field[]> methodFieldMappings = new HashMap<>();
	
	private static CurrentUserProvider currentUserProvider;
	
	
	public static CurrentUserProvider getCurrentUserProvider() {
		if(currentUserProvider == null && !methodFieldMappings.isEmpty()) {
			synchronized (AutoFieldFillHandler.class) {
				currentUserProvider = InstanceFactory.getInstance(CurrentUserProvider.class);
				if(currentUserProvider == null) {
					methodFieldMappings.clear();
				}
			}
		}
		return currentUserProvider;
	}

	@Override
	public void start(JeesuiteMybatisInterceptor context) {
		List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos(context.getGroupName());
		
		for (EntityInfo ei : entityInfos) {
			Field[] createdFields = new Field[3];
			Field[] updatedFields = new Field[2];
			Field[] fields = FieldUtils.getAllFields(ei.getEntityClass());
			for (Field field : fields) {
				if(field.isAnnotationPresent(CreatedBy.class)) {
					field.setAccessible(true);
					createdFields[0] = field;
				}else if(field.isAnnotationPresent(CreatedAt.class)) {
					field.setAccessible(true);
					createdFields[1] = field;
				}else if(field.isAnnotationPresent(UpdatedBy.class)) {
					field.setAccessible(true);
					updatedFields[0] = field;
				}else if(field.isAnnotationPresent(UpdatedAt.class)) {
					field.setAccessible(true);
					updatedFields[1] = field;
				}else if(field.isAnnotationPresent(TenantShardding.class)) {
					field.setAccessible(true);
					createdFields[2] = field;
				}
			}
	
			String keyPrefix = ei.getMapperClass().getName() + ".";
			if(createdFields[0] != null || createdFields[1] != null) {
	        	methodFieldMappings.put(keyPrefix + "insert", createdFields);
	        	methodFieldMappings.put(keyPrefix + "insertSelective", createdFields);
	        	methodFieldMappings.put(keyPrefix + INSERT_LIST_METHOD_NAME, createdFields);
	        }
			if(updatedFields[0] != null || updatedFields[1] != null) {
	        	methodFieldMappings.put(keyPrefix + "updateByPrimaryKey", updatedFields);
	        	methodFieldMappings.put(keyPrefix + "updateByPrimaryKeySelective", updatedFields);
	        	methodFieldMappings.put(keyPrefix + "updateByPrimaryKeyWithVersion", updatedFields);
	        }
		}
        
	}


	@SuppressWarnings("unchecked")
	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
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
		CurrentUserProvider userProvider = getCurrentUserProvider();
		Date currentTime = new Date();
		if(userProvider != null && fields[0] != null) {
			try {fields[0].set(parameter, userProvider.currentUser());} catch (Exception e) {}
		}
		if(fields[1] != null) {
			try {fields[1].set(parameter, currentTime);} catch (Exception e) {}
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
