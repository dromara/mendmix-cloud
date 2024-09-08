/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.springweb.exception;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.model.WrapperResponse;
import org.dromara.mendmix.springweb.i18n.I18nMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {
	
	@Autowired(required = false)
	private SpecExceptionHandler specExceptionHandler;
	@Autowired(required = false)
	private ExceptionResponseConverter exceptionResponseConverter;

	@ExceptionHandler(Exception.class)
	@ResponseBody
	public Object exceptionHandler(HttpServletRequest request, HttpServletResponse response,Exception e) {

		WrapperResponse<?> resp = new WrapperResponse<>();
		Throwable throwable = getActualThrowable(e);
		if(throwable instanceof Exception == false) {
			e.printStackTrace();
			throwable = new MendmixBaseException(500, e.getMessage());
		}
		if(throwable instanceof Exception) {
			e = (Exception) throwable;
		}else {
			e.printStackTrace();
			e = new MendmixBaseException(500, e.getMessage());
		}
		if (e instanceof MendmixBaseException) {
			MendmixBaseException e1 = (MendmixBaseException) e;
			resp.setCode(e1.getCode());
			if(e1.getBizCode() != null) {
				resp.setMsg(getLocaleMesage(e1.getBizCode(), e1.getMessage()));
			}else {
				resp.setMsg(e1.getMessage());
			}
			resp.setContextParams(e1.getContextParam());
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
				fieldName = parseFieldName(error);
				fieldNames.append(fieldName).append(",");
			}
			resp.setBizCode("error.parameter.notValid");
			resp.setMsg(getLocaleMesage("error.parameter.notValid", "参数错误"));
		} else {
			if(specExceptionHandler == null || !specExceptionHandler.handle(resp, throwable)) {
				Throwable parent = e.getCause();
				if (parent instanceof IllegalStateException) {
					resp.setCode(501);
					resp.setMsg(e.getMessage());
				} else {
					resp.setCode(500);
					resp.setMsg(getLocaleMesage("error.global.system.error", "系统繁忙,请稍后再试"));
				}
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
		ThreadLocalContext.set(GlobalConstants.CONTEXT_EXCEPTION, throwable);
		//
		if(exceptionResponseConverter != null) {
			return exceptionResponseConverter.convert(resp);
		}
		return resp.withContextParam();
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
	
	private String getLocaleMesage(String code,String defaultMessage) {
		String localeMessage = I18nMessageUtils.getMessage(code);
		return StringUtils.defaultString(localeMessage, defaultMessage);
	}
}