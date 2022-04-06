package com.jeesuite.springweb.exception;

import java.lang.reflect.Method;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.model.WrapperResponse;
import com.jeesuite.logging.integrate.ActionLogCollector;

@ControllerAdvice
public class GlobalExceptionHandler {

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
	public WrapperResponse<?> exceptionHandler(Exception e, HttpServletResponse response) {

		// 缓存回滚
		if (rollbackCacheMethod != null) {
			try {
				rollbackCacheMethod.invoke(null);
			} catch (Exception e2) {
			}
		}

		WrapperResponse<?> resp = new WrapperResponse<>();
		
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
		} else if(e instanceof MethodArgumentNotValidException){
			List<ObjectError> errors = ((MethodArgumentNotValidException)e).getBindingResult().getAllErrors();
			StringBuffer errorMsg=new StringBuffer();
	        errors.stream().forEach(x -> errorMsg.append(x.getDefaultMessage()).append(";"));
	        resp.setCode(400);
			resp.setMsg(errorMsg.toString());
		} else {
			Throwable parent = e.getCause();
			if (parent instanceof IllegalStateException) {
				resp.setCode(501);
				resp.setMsg(e.getMessage());
			} else {
				resp.setCode(500);
				resp.setMsg("系统繁忙");
			}
		}

		//默认情况http code都转换为200，异常信息由异常体传递
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		if(request != null && Boolean.parseBoolean(request.getHeader(CustomRequestHeaders.HEADER_HTTP_STATUS_KEEP))){
			int errorCode = resp.getCode();
			if(errorCode >= 400 && errorCode<=500){
				response.setStatus(errorCode);
			}else{
				response.setStatus(500);
			}
		}
		response.addIntHeader(CustomRequestHeaders.HEADER_EXCEPTION_CODE, resp.getCode());
		response.addHeader(CustomRequestHeaders.HEADER_RESP_KEEP, Boolean.TRUE.toString());
		//
		ActionLogCollector.onResponseEnd(response, e);
		
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