/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.springweb.enhancer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.mendmix.common.CustomRequestHeaders;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.model.WrapperResponse;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.PathMatcher;
import com.mendmix.springweb.AppConfigs;

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
public class ResonseBodyEnhancerAdvice implements ResponseBodyAdvice<Object>,InitializingBean {

	private static List<ResponseBodyEnhancer> enhancers = new ArrayList<>();
	
	public static void register(ResponseBodyEnhancer enhancer) {
		enhancers.add(enhancer);
		if(enhancers.size() > 1) {			
			enhancers = enhancers.stream().sorted(Comparator.comparing(ResponseBodyEnhancer::order)).collect(Collectors.toList());
		}
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if(AppConfigs.respRewriteEnabled) {
			register(new ResponseRewrite());
		}
	}
	
    @Override
    public boolean supports(MethodParameter methodParameter,
                            Class<? extends HttpMessageConverter<?>> aClass) {
        return !enhancers.isEmpty(); // methodParameter.hasMethodAnnotation(ResponseBody.class);
    }


    @Override
    public Object beforeBodyWrite(Object body,
    		MethodParameter methodParameter,
            MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> aClass,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if(body != null && body instanceof Map) {
        	String statusValue = Objects.toString(((Map<?, ?>)body).get("status"), null);
        	if(String.valueOf(HttpStatus.NOT_FOUND.value()).equals(statusValue)) {
        		return body;
        	}
        }
    	for (ResponseBodyEnhancer enhancer : enhancers) {
			body = enhancer.process(body, methodParameter, mediaType, aClass,request,response);
		}
   
		return body;
    }

    private class ResponseRewrite implements ResponseBodyEnhancer {

    	private PathMatcher ignorePathMatcher = new PathMatcher(GlobalRuntimeContext.getContextPath(), "/actuator/*");
		@SuppressWarnings("unchecked")
		@Override
		public Object process(Object body,
	    		MethodParameter methodParameter,
	            MediaType mediaType,
	            Class<? extends HttpMessageConverter<?>> aClass,
	            ServerHttpRequest request,
	            ServerHttpResponse response) {
			
			if(response.getHeaders().containsKey(CustomRequestHeaders.HEADER_RESP_KEEP) 
					|| request.getHeaders().containsKey(CustomRequestHeaders.HEADER_RESP_KEEP)) {
	        	return body;
	        }
	    	
	    	if(body instanceof WrapperResponse) {
	    		response.getHeaders().add(CustomRequestHeaders.HEADER_RESP_KEEP, Boolean.TRUE.toString());
	        	return body;
	        }
	    	
	    	if(body instanceof Map) {
	    		Map<String, Object> bodyToMap = (Map<String, Object>) body;
	    		//{timestamp=Sun May 29 15:37:57 CST 2022, status=500, error=Internal Server Error, path=/api/svc/user/basic/1}
	    		if(bodyToMap.containsKey("status") && bodyToMap.containsKey("error")) {
	    			return WrapperResponse.fail((int)bodyToMap.get("status"), bodyToMap.get("error").toString());
	    		}
	        }
	    	
	    	if(ignorePathMatcher.match(request.getURI().getPath())) {
	    		response.getHeaders().add(CustomRequestHeaders.HEADER_RESP_KEEP, Boolean.TRUE.toString());
	    		return body;
	    	}
	    	
	    	if(!mediaType.includes(MediaType.APPLICATION_JSON) && !methodParameter.hasMethodAnnotation(ResponseBody.class)) {
	    		return body;
	        }
	    	//
	    	response.getHeaders().add(CustomRequestHeaders.HEADER_RESP_KEEP, Boolean.TRUE.toString());
	    	WrapperResponse<Object> rewriteBody = new WrapperResponse<Object>(body);
	    	//
	    	if(StringHttpMessageConverter.class == aClass) {
	    		response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
	    		return JsonUtils.toJson(rewriteBody);
	    	}
	    	
			return rewriteBody;
		}

		@Override
		public int order() {
			return 99;
		}
    	
    }

}

