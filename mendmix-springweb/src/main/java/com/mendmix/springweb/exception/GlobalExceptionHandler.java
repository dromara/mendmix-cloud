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
package com.mendmix.springweb.exception;

import java.lang.reflect.Method;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mendmix.common.CustomRequestHeaders;
import com.mendmix.common.GlobalConstants;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.model.WrapperResponse;
import com.mendmix.logging.integrate.ActionLogCollector;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static Method rollbackCacheMethod;

	static {
		try {
			Class<?> cacheHandlerClass = Class.forName("com.mendmix.mybatis.plugin.cache.CacheHandler");
			rollbackCacheMethod = cacheHandlerClass.getMethod("rollbackCache");
		} catch (Exception e) {
		}
	}

	@ExceptionHandler(Exception.class)
	@ResponseBody
	public WrapperResponse<?> exceptionHandler(HttpServletRequest request, HttpServletResponse response,Exception e) {

		// 缓存回滚
		if (rollbackCacheMethod != null) {
			try {
				rollbackCacheMethod.invoke(null);
			} catch (Exception e2) {
			}
		}

		WrapperResponse<?> resp = new WrapperResponse<>();
		
		e = (Exception) getActualThrowable(e);
		if (e instanceof MendmixBaseException) {
			MendmixBaseException e1 = (MendmixBaseException) e;
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
			resp.setCode(400);
			List<ObjectError> errors = ((MethodArgumentNotValidException)e).getBindingResult().getAllErrors();
			
			String fieldName;
			StringBuilder fieldNames = new StringBuilder();
			for (ObjectError error : errors) {
				String errMsg =  error.getDefaultMessage();
				fieldName = parseFieldName(error);
				fieldNames.append(fieldName).append(",");
			}
			resp.setBizCode("error.parameter.notValid");
			resp.setMsg("参数错误["+fieldNames.toString()+"]");
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
		if(Boolean.parseBoolean(request.getHeader(CustomRequestHeaders.HEADER_HTTP_STATUS_KEEP))){
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
		ActionLogCollector.onResponseEnd(response.getStatus(), e);
		
		return resp;
	}
	
	private Throwable getActualThrowable(Throwable e){
		Throwable cause = e;
		while(cause.getCause() != null){
			cause = cause.getCause();
		}
		return cause;
	}
	
	private String parseFieldName(ObjectError error) {
		String[] codes = error.getCodes();
		if(codes.length >= 2) {
			return StringUtils.split(codes[1], GlobalConstants.DOT)[1];
		}
		if(codes.length >= 1) {
			return StringUtils.split(codes[0], GlobalConstants.DOT)[2];
		}
		return error.getCode();
	}
}