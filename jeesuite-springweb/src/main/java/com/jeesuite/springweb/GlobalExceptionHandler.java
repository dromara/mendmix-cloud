package com.jeesuite.springweb;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.springweb.model.WrapperResponseEntity;
import com.jeesuite.springweb.utils.WebUtils;

@ControllerAdvice
public class GlobalExceptionHandler {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static Method rollbackCacheMethod;

	static {
		try {
			Class<?> cacheHandlerClass = Class.forName("com.jeesuite.mybatis.plugin.cache.CacheHandler");
			rollbackCacheMethod = cacheHandlerClass.getMethod("rollbackCache");
		} catch (Exception e) {
		}
	}

	@ExceptionHandler(Exception.class)
	@ResponseBody
	public WrapperResponseEntity exceptionHandler(Exception e, HttpServletResponse response) {

		// 缓存回滚
		if (rollbackCacheMethod != null) {
			try {
				rollbackCacheMethod.invoke(null);
			} catch (Exception e2) {
			}
		}

		WrapperResponseEntity resp = new WrapperResponseEntity();
		
		e = (Exception) getActualThrowable(e);
		if (e instanceof JeesuiteBaseException) {
			JeesuiteBaseException e1 = (JeesuiteBaseException) e;
			resp.setCode(e1.getCode());
			resp.setMsg(e1.getMessage());
		} else if (e instanceof org.springframework.web.HttpRequestMethodNotSupportedException) {
			resp.setCode(HttpStatus.METHOD_NOT_ALLOWED.value());
			resp.setMsg(e.getMessage());
		} else if (e instanceof org.springframework.web.HttpMediaTypeException) {
			resp.setCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
			resp.setMsg(e.getMessage());
		} else if (e instanceof org.springframework.web.bind.MissingServletRequestParameterException) {
			resp.setCode(1001);
			resp.setMsg(e.getMessage());
		} else {
			Throwable parent = e.getCause();
			if (parent instanceof IllegalStateException) {
				resp.setCode(501);
				resp.setMsg(e.getMessage());
			} else {
				resp.setCode(500);
				resp.setMsg("系统繁忙");
			}
			logger.error("", e);
		}

		if(WebUtils.isInternalRequest(RequestContextHelper.getRequest())){
			int errorCode = resp.getCode();
			if(errorCode >= 400 && errorCode<=500){
				response.setStatus(errorCode);
			}else{
				response.setStatus(500);
			}
		}
		
		return resp;
	}
	
	private Throwable getActualThrowable(Throwable e){
		Throwable cause = e;
		while(cause.getCause() != null){
			cause = cause.getCause();
		}
		return cause;
	}
}