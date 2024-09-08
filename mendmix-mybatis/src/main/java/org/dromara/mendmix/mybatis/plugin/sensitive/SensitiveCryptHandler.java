package org.dromara.mendmix.mybatis.plugin.sensitive;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.crypt.CustomEncryptor;
import org.dromara.mendmix.common.util.BeanUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.common.util.TimeConvertUtils;
import org.dromara.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.PluginInterceptorHandler;
import org.dromara.mendmix.mybatis.plugin.sensitive.annotation.SensitiveField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年8月10日
 */
public class SensitiveCryptHandler implements PluginInterceptorHandler {

    private static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");
    
    public static final String CONTEXT_KEY = "_ctx_sensitiveCrypt";
	
	private CustomEncryptor encryptor;
	
	private List<String> defaultFieldNames;
	
	
	@Override
	public void start(MendmixMybatisInterceptor context) {
		String cryptKey = ResourceUtils.getProperty("mendmix-cloud.sensitive.cryptKey",GlobalContext.APPID + GlobalContext.ENV);
		encryptor = new CustomEncryptor(cryptKey, false);
		if(ResourceUtils.containsProperty("mendmix-cloud.sensitive.defaultFieldNames")) {
			defaultFieldNames = ResourceUtils.getList("mendmix-cloud.sensitive.defaultFieldNames");
		}else {
			defaultFieldNames = ResourceUtils.getList("mendmix-cloud.mybatis.sensitive.defaultFieldNames");
		}
		logger.info("SensitiveCrypt defaultFieldNames -> {}",defaultFieldNames);
	}

	@Override
	public void close() {}

	@Override
	public Object onInterceptor(OnceContextVal invocationVal) throws Throwable {
		if(!ThreadLocalContext.exists(CONTEXT_KEY))return null;
		MappedStatement statement = invocationVal.getMappedStatement();
		if(statement.getSqlCommandType() == SqlCommandType.INSERT 
				|| statement.getSqlCommandType() == SqlCommandType.UPDATE) {
			handleObjectEncryt(invocationVal.getParameter());
		}
		return null;
	}

	@Override
	public void onFinished(OnceContextVal invocationVal, Object result) {
		if(!ThreadLocalContext.exists(CONTEXT_KEY))return;
		if(!invocationVal.isSelect() 
				|| invocationVal.getMappedStatement().getId().contains(SelectKeyGenerator.SELECT_KEY_SUFFIX)) {
			return;
		}
		try {
			handleObjectDecrypt(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int interceptorOrder() {
		return 2;
	}
	
	/**
	 * @param result
	 * @throws Exception 
	 */
	private void handleObjectDecrypt(Object o) throws Exception {
		if(o == null || BeanUtils.isSimpleDataType(o))return;
		if(o instanceof Collection) {
			Collection<?> list = (Collection<?>)o;
			for (Object object : list) {
				handleObjectDecrypt(object);
			}
		}else {
			Field[] fields = FieldUtils.getAllFields(o.getClass());
			Object value;
			for (Field field : fields) {
				if(field.isEnumConstant())continue;
				if(Collection.class.isAssignableFrom(field.getType())) {
					field.setAccessible(true);
					value = field.get(o);
					if(value == null)continue;
					for (Object object : (Collection<?>)value) {
						handleObjectDecrypt(object);
					}
				}else if(field.isAnnotationPresent(SensitiveField.class)) {
					field.setAccessible(true);
					value = field.get(o);
					String valueString = Objects.toString(value, null);
					if(StringUtils.isBlank(valueString) 
							|| !valueString.startsWith(GlobalConstants.CRYPT_PREFIX))continue;
					
					value = encryptor.decrypt(valueString);
					field.set(o, value); 
				}else if(!BeanUtils.isSimpleDataType(field.getType())) {
					field.setAccessible(true);
					value = field.get(o);
					handleObjectDecrypt(value);
				}
			}
		}
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void handleObjectEncryt(Object parameter) {
		if (parameter instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) parameter;
			if(map.containsKey("arg0")) {
				handleObjectItemEncrypt(map.get("arg0"));
			}else {
				for (Object object : map.values()) {
					handleObjectItemEncrypt(object);
				}
			}
		}else if(parameter instanceof Collection) {
			for (Object object : (Collection) parameter) {
				handleObjectItemEncrypt(object);
			}
		}else {
			handleObjectItemEncrypt(parameter);
		}
	}
	
	
	/**
	 * @param parameter
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void handleObjectItemEncrypt(Object parameter) {
		if(BeanUtils.isSimpleDataType(parameter))return;
		if(parameter instanceof Map) {
			Map map = (Map) parameter;
			for (String fieldName : defaultFieldNames) {
				Object value = map.get(fieldName);
				if(value == null)continue;
				if(value instanceof Date) {
					value = TimeConvertUtils.toUTC((Date)value);
					map.put(fieldName, value);
				}else if(value instanceof String) {
					value = TimeConvertUtils.toUTC(value.toString());
					map.put(fieldName, value);
				}
			}
		}else if(parameter instanceof Collection) {
			for (Object object : (Collection) parameter) {
				handleObjectItemEncrypt(object);
			}
		}else {
			List<Field> fields = FieldUtils.getFieldsListWithAnnotation(parameter.getClass(), SensitiveField.class);
			for (Field field : fields) {
				field.setAccessible(true);
			}
			if(!defaultFieldNames.isEmpty()) {
				Field field;
				for (String fieldName : defaultFieldNames) {
					field = FieldUtils.getField(parameter.getClass(), fieldName, true);
					if(field != null)fields.add(field);
				}
			}
			
			for (Field field : fields) {
				try {
					Object value = field.get(parameter);
					String valueString = Objects.toString(value, null);
					if(StringUtils.isBlank(valueString) 
							|| valueString.startsWith(GlobalConstants.CRYPT_PREFIX))continue;
					value = encryptor.encrypt(valueString);
					field.set(parameter, value);
				} catch (Exception e) {
					logger.error("["+field.getName()+"]加密错误",e);
				} 
			}
		}
	}

}
