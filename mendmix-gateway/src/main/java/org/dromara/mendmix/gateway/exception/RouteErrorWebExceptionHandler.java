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
package org.dromara.mendmix.gateway.exception;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.util.ExceptionFormatUtils;
import org.dromara.mendmix.common.util.LogMessageFormat;
import org.dromara.mendmix.gateway.filter.pre.FallbackBreakerHandler;
import org.dromara.mendmix.gateway.helper.RequestContextHelper;
import org.dromara.mendmix.gateway.model.BizSystemModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date May 26, 2022
 */
public class RouteErrorWebExceptionHandler  extends DefaultErrorWebExceptionHandler {

	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.gateway");
	
	@Autowired(required = false)
	private FallbackBreakerHandler fallbackHandler;
	
	public RouteErrorWebExceptionHandler(ErrorAttributes errorAttributes, Resources resources,
			ErrorProperties errorProperties, ApplicationContext applicationContext) {
		super(errorAttributes, resources, errorProperties, applicationContext);
	}


	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
		
		Throwable error = ExceptionUtils.getRootCause(super.getError(request));
		final String exceptionDetails = ExceptionFormatUtils.buildExceptionMessages(error, 5);
		if(request.exchange().getAttributes().containsKey(GlobalConstants.DEBUG_TRACE_PARAM_NAME)) {
			logger.error("routeError"+LogMessageFormat.buildLogTail(),error);
		}else {
			logger.warn("routeError{},url:{},errorMsg:{}",LogMessageFormat.buildLogTail(),request.path(),exceptionDetails);
		}
		String msg = StringUtils.defaultString(error.getMessage(), "系统繁忙");
		Integer code = HttpStatus.INTERNAL_SERVER_ERROR.value();
		if(error.getClass().getName().contains("UnknownHostException") 
				|| msg.contains("Connection refused") 
				|| msg.contains("Failed to resolve '")
				|| msg.contains("connection timed out")
				|| error instanceof java.net.UnknownHostException
				|| error instanceof io.netty.channel.ConnectTimeoutException) {
			msg = "Service Unavailable";
			code = 503;
		}
	
		Map<String, Object> fallbackData = null;
		if(code == 503 && fallbackHandler != null) {
			fallbackData = (Map<String, Object>) fallbackHandler.handle(request.exchange(),false);
		}
		
		Map<String, Object> errorAttributes;
		if(fallbackData != null) {
			errorAttributes = fallbackData;
		}else {
			errorAttributes = new HashMap<>(3);
			errorAttributes.put(GlobalConstants.PARAM_MSG, msg);
			errorAttributes.put(GlobalConstants.PARAM_CODE, code);
			//
			Map<String, String> data = new HashMap<>(3);
			BizSystemModule module = RequestContextHelper.getCurrentModule(request.exchange());
			data.put("serviceId", module.getServiceId());
			data.put("path", request.path());
			data.put("exception", exceptionDetails);
			errorAttributes.put("extraMsg", data);
		}
		//
		RequestContextHelper.clearCustomContextAttributes(request.exchange());
		
		return errorAttributes;

	}

	@Override
	protected int getHttpStatus(Map<String, Object> errorAttributes) {
		if(errorAttributes != null && errorAttributes.containsKey(GlobalConstants.PARAM_CODE)) {
			return (int)errorAttributes.get(GlobalConstants.PARAM_CODE) ;
		}
		return HttpStatus.INTERNAL_SERVER_ERROR.value();
	}

}
