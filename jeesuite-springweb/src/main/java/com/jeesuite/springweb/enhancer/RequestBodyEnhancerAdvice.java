/*
 * Copyright 2016-2022 www.jeesuite.com.
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
package com.jeesuite.springweb.enhancer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.spring.InstanceFactory;

/**
 * 
 * <br>
 * Class Name   : RequestBodyEnhancerAdvice
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Feb 22, 2022
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestBodyEnhancerAdvice implements RequestBodyAdvice,InitializingBean {

	private static List<RequestBodyEnhancer> enhancers = new ArrayList<>();
	
	public static void register(RequestBodyEnhancer enhancer) {
		enhancers.add(enhancer);
		if(enhancers.size() > 1) {			
			enhancers.stream().sorted(Comparator.comparing(RequestBodyEnhancer::order));
		}
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		Map<String, RequestBodyEnhancer> beans = InstanceFactory.getBeansOfType(RequestBodyEnhancer.class);
		for (RequestBodyEnhancer enhancer : beans.values()) {
			register(enhancer);
		}
	}
	
	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {
		return !enhancers.isEmpty();
	}

	
	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
		return null;
	}

	
	@Override
	public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {
		
		HttpServletRequest request = CurrentRuntimeContext.getRequest();
		for (RequestBodyEnhancer enhancer : enhancers) {
			body = enhancer.process(request,body, parameter);
		}
		return body;
	}

	
	@Override
	public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
		return null;
	}

}
