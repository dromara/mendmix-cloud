package org.dromara.mendmix.mybatis.plugin.timezone;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.SqlCommandType;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.util.BeanUtils;
import org.dromara.mendmix.common.util.DateUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.common.util.TimeConvertUtils;
import org.dromara.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.PluginInterceptorHandler;
import org.dromara.mendmix.mybatis.plugin.timezone.annotation.UTCTimeConvert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年7月26日
 */
public class TimeZoneConvertHandler implements PluginInterceptorHandler {
	
	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");
	
	public static final String CONTEXT_KEY = "_ctx_timeZoneConvert";
	
	private static Pattern paramPattern = Pattern.compile("param[1-9]{1}");
	
	private List<String> defaultFieldNames;
	
	@Override
	public void start(MendmixMybatisInterceptor context) {
		if(ResourceUtils.containsProperty("mendmix-cloud.timezone.convert.defaultFieldNames")) {
			defaultFieldNames = ResourceUtils.getList("mendmix-cloud.timezone.convert.defaultFieldNames");
		}else {
			defaultFieldNames = ResourceUtils.getList("mendmix-cloud.mybatis.timezone.convert.defaultFieldNames");
		}
		logger.info("TimeZoneConvert defaultFieldNames -> {}",defaultFieldNames);
	}

	@Override
	public void close() {
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object onInterceptor(OnceContextVal invocation) throws Throwable {
		if(invocation.getMappedStatement().getSqlCommandType() == SqlCommandType.DELETE) {
			return null;
		}
		if(logger.isDebugEnabled()) {
			logger.debug("{} handleTimeToUTC Begin....",invocation.getMappedStatement().getId());
		}
		//
		boolean updateAction = invocation.getMappedStatement().getSqlCommandType() == SqlCommandType.UPDATE;
		Object parameter = invocation.getParameter();
		if (parameter instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) parameter;
			if(map.containsKey("arg0")) {
				handleTimeToUTC(map.get("arg0"),updateAction);
			}else {
				for (String key : map.keySet()) {
					if(paramPattern.matcher(key).matches())continue;
					handleTimeToUTC(map.get(key),updateAction);
				}
			}
		}else if(parameter instanceof Collection) {
			for (Object object : (Collection) parameter) {
				handleTimeToUTC(object,updateAction);
			}
		}else {
			handleTimeToUTC(parameter,updateAction);
		}

		return null;
	}


	@Override
	public void onFinished(OnceContextVal invocationVal, Object result) {
		
		if(!invocationVal.isSelect() 
				|| invocationVal.getMappedStatement().getId().contains(SelectKeyGenerator.SELECT_KEY_SUFFIX)) {
			return;
		}
		
		if(logger.isDebugEnabled()) {
			logger.debug("{} handleReverseFromUTC Begin....",invocationVal.getMappedStatement().getId());
		}
		
		String timeZone = CurrentRuntimeContext.getTimeZone();
		if(StringUtils.isNotBlank(timeZone)) {
			try {
				handleReverseFromUTC(result,timeZone);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public int interceptorOrder() {
		return 2;
	}
	
	/**
	 * @param parameter
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void handleTimeToUTC(Object parameter,boolean updateAction) {
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
				handleTimeToUTC(object,updateAction);
			}
		}else {
			Field[] fields = FieldUtils.getAllFields(parameter.getClass());
			Date date = new Date();
			UTCTimeConvert annotation;
			for (Field field : fields) {
				if(field.isAnnotationPresent(UTCTimeConvert.class)) {
					annotation = field.getAnnotation(UTCTimeConvert.class);
					if(TimeConvertUtils.isSystemDefaultUTC() && annotation.systemTime()) {
						continue;
					}
					field.setAccessible(true);
					doConvertUTC(parameter, field, date, annotation.systemTime(),updateAction);
				}else if(!TimeConvertUtils.isSystemDefaultUTC() && defaultFieldNames.contains(field.getName())) {
					field.setAccessible(true);
					doConvertUTC(parameter, field, date, true,updateAction);
				}
			}
		}
	}
	
	/**
	 * 
	 * @param o
	 * @param timeZone
	 * @throws Exception
	 */
	private void handleReverseFromUTC(Object o,String timeZone) throws Exception {
		if(o == null || BeanUtils.isSimpleDataType(o))return;
		if(o instanceof Collection) {
			Collection<?> list = (Collection<?>)o;
			for (Object object : list) {
				handleReverseFromUTC(object,timeZone);
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
						handleReverseFromUTC(object,timeZone);
					}
				}else if(field.isAnnotationPresent(UTCTimeConvert.class) || defaultFieldNames.contains(field.getName())) {
					field.setAccessible(true);
					value = field.get(o);
					if(value == null || StringUtils.isBlank(value.toString())) {
						continue;
					}
					if(value instanceof Date) {
						value = TimeConvertUtils.reverseFromUTC((Date)value, timeZone);
					}else {
						value = TimeConvertUtils.reverseFromUTC(value.toString(), timeZone);
					}
					field.set(o, value); 
				}else if(!BeanUtils.isSimpleDataType(field.getType())) {
					field.setAccessible(true);
					value = field.get(o);
					handleReverseFromUTC(value,timeZone);
				}
			}
		}
	}
	
	
	private void doConvertUTC(Object o, Field field, Date currentTime, boolean isSystemTime,boolean isUpdate) {
		try {
			Object value = field.get(o);
			if (value == null)
				return;
			String timeZone = CurrentRuntimeContext.getTimeZone();
			Date utcDate = null;
			if (value instanceof Date) {
				if (isSystemTime) {
					if(isUpdate && StringUtils.isNotBlank(timeZone) && DateUtils.getDiffSeconds(currentTime, (Date)value) > 600) {
						utcDate = TimeConvertUtils.toUTC((Date) value, timeZone);
					}else if(!TimeConvertUtils.isSystemDefaultUTC()) {
						utcDate = TimeConvertUtils.toUTC((Date) value);
					}
				}else if (StringUtils.isNotBlank(timeZone)){					
					utcDate = TimeConvertUtils.toUTC((Date) value, timeZone);
				}
				if(utcDate != null) {
					field.set(o, utcDate);
					if (logger.isDebugEnabled()) {
						logger.debug(">> Convert {}.{} oldValue:{},newValue:{},isSystemTime:{}", o.getClass().getSimpleName(),
								field.getName(), value, utcDate,isSystemTime);
					}
				}
				
			}
		} catch (Exception e) {
			logger.error("[" + field.getName() + "]时区转换错误", e);
		}
	}

}
