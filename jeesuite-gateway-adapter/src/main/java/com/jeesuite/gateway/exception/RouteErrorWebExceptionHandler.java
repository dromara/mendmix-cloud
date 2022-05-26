/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.jeesuite.gateway.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;

import com.jeesuite.common.GlobalConstants;
import com.jeesuite.gateway.CurrentSystemHolder;
import com.jeesuite.gateway.helper.RuequestHelper;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.logging.helper.LogMessageFormat;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date May 26, 2022
 */
public class RouteErrorWebExceptionHandler  extends DefaultErrorWebExceptionHandler {

	private final static Logger logger = LoggerFactory.getLogger("com.jeesuite.gateway");
	
	public RouteErrorWebExceptionHandler(ErrorAttributes errorAttributes, Resources resources,
			ErrorProperties errorProperties, ApplicationContext applicationContext) {
		super(errorAttributes, resources, errorProperties, applicationContext);
	}
	
	@Override
	protected Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
		
		Throwable error = super.getError(request);
		
		logger.warn("routeError{},errorType:{},errorMsg:{}",LogMessageFormat.buildLogTail(request.path()),error.getClass().getName(),error.getMessage());
		String msg = error.getMessage();
		Integer code = HttpStatus.INTERNAL_SERVER_ERROR.value();
		if(msg.contains("Connection refused") || msg.contains("connection timed out")) {
			msg = "Service Unavailable";
			code = 503;
		}
	
		Map<String, Object> errorAttributes = new HashMap<>(3);
		errorAttributes.put(GlobalConstants.PARAM_MSG, msg);
		errorAttributes.put(GlobalConstants.PARAM_CODE, code);
		Map<String, String> data = new HashMap<>(2);
		String routeName = RuequestHelper.resolveRouteName(request.path());
		BizSystemModule module = CurrentSystemHolder.getModule(routeName);
		data.put("serviceId", module.getServiceId());
		data.put("path", request.path());
		errorAttributes.put(GlobalConstants.PARAM_DATA, data);
		
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
