package com.jeesuite.springweb.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.util.BeanUtils;
import com.jeesuite.springweb.annotation.ApiMetadata;
import com.jeesuite.springweb.logging.ActionLog;
import com.jeesuite.springweb.logging.RequestLogBuilder;
import com.jeesuite.springweb.logging.RequestLogCollector;

@Aspect
@Component
@ConditionalOnProperty(value = "request.log.enabled",havingValue = "true")
public class RequestLoggingInterceptor {

private static Logger log = LoggerFactory.getLogger("global.request.logger");
	
	private Map<String, List<ParameterLogConfig>> parameterLogConfigs = new HashMap<>();

	@Value("${request.log.responseBody.ignore:true}")
	private boolean ignoreResponseBody;
	

	@Pointcut("execution(public * com.zvos..controller.*.*(..)) || execution(public * com.zvos..web.*.*(..))")
	public void pointcut() {}
	
	@Around("pointcut()")
	public Object around(ProceedingJoinPoint pjp) throws Throwable {
		
		ActionLog actionLog = RequestLogCollector.currentActionLog();
		
		if(actionLog == null) {
			return pjp.proceed();
		}
		
		HttpServletRequest request = CurrentRuntimeContext.getRequest();
		
		Method method = ((MethodSignature)pjp.getSignature()).getMethod();  
		
		boolean requestLog = true;
		boolean responseLog = !ignoreResponseBody;
		if(method.isAnnotationPresent(ApiMetadata.class)) {
			ApiMetadata metadata = method.getAnnotation(ApiMetadata.class);
			requestLog = metadata.requestLog();
			responseLog = metadata.responseLog();
			if(!requestLog && !responseLog) {
				return pjp.proceed();
			}
		}
		
		Map<String, String> parameters = null;
		Object body = null;

		if(requestLog) {
			Object[] args = pjp.getArgs();
			if(args.length > 0) {
				List<ParameterLogConfig> parameterConfigs = getParameterConfigs(pjp.getTarget().getClass().getName(),method);
				
				parameters = new HashMap<>(3);
				ParameterLogConfig config;
				for (int i = 0; i < args.length; i++) {
					if((config = parameterConfigs.get(i)).ignore)continue;
					if(config.isBody) {
						body = args[i];
					} else if(config.paramName != null && args[i] != null){
						parameters.put(config.paramName, args[i].toString());
					}

				}
			}
			if(log.isDebugEnabled()) {
				String requestLogMessage = RequestLogBuilder.requestLogMessage(request.getRequestURI(), request.getMethod(), parameters, body);
				log.debug(requestLogMessage);
			}
		}
		
		actionLog.setRequestParameters(parameters);
		actionLog.setRequestData(body);
		
		Object result = null;
		try {
			result = pjp.proceed();
            //
			if(responseLog) {
				actionLog.setResponseData(result);
			}
			return result;
		} catch (Exception e) {
			if (e instanceof JeesuiteBaseException) {
				actionLog.setExceptions(e.getMessage());
			}else {
				actionLog.setExceptions(ExceptionUtils.getMessage(e));
			}
			throw e;
		}
	}
	
private List<ParameterLogConfig> getParameterConfigs(String className,Method method) {
		
		String fullName = className + method.getName();
		
		List<ParameterLogConfig> configs = parameterLogConfigs.get(fullName);
		//
        if(configs == null) {
        	synchronized (parameterLogConfigs) {
        		Parameter[] parameters = method.getParameters() == null ? new Parameter[0] : method.getParameters();
        		
        		configs = new ArrayList<>(parameters.length);
        		ParameterLogConfig config;
        		for (Parameter parameter : parameters) {
        			config = new ParameterLogConfig();
        			if(ServletRequest.class.isAssignableFrom(parameter.getType()) 
        					|| ServletResponse.class.isAssignableFrom(parameter.getType())) {
        				config.ignore = true;
        				configs.add(config);
        				continue;
        			}
        			String paramName = null;
        			if(parameter.isAnnotationPresent(RequestParam.class)) {
        				paramName = parameter.getAnnotation(RequestParam.class).value();
        				if(StringUtils.isBlank(paramName)) {
        					paramName = parameter.getName();
        				}
        			}else if(parameters.length > 1) {
        				paramName = parameter.getName();
        			}
        			config.paramName = paramName;
        			config.isBody = parameter.getType() != MultipartFile.class && !BeanUtils.isSimpleDataType(parameter.getType());
        			configs.add(config);
        		}
        		
        		parameterLogConfigs.put(fullName, configs);
			}
        }
		
		return configs;
	}

private class ParameterLogConfig {
	boolean ignore;
	boolean isBody;
	String paramName;
}
}
