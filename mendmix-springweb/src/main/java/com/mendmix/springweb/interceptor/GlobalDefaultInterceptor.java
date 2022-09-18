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
package com.mendmix.springweb.interceptor;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.CustomRequestHeaders;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.annotation.ApiMetadata;
import com.mendmix.common.exception.ForbiddenAccessException;
import com.mendmix.common.util.PathMatcher;
import com.mendmix.common.util.TokenGenerator;
import com.mendmix.common.util.WebUtils;
import com.mendmix.logging.actionlog.ActionLogCollector;
import com.mendmix.springweb.AppConfigs;

/**
 * 
 * 
 * <br>
 * Class Name   : GlobalDefaultInterceptor
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年07月1日
 */
public class GlobalDefaultInterceptor implements HandlerInterceptor {

	private static Logger log = LoggerFactory.getLogger("com.mendmix.springweb");

	private PathMatcher invoketokenCheckIgnoreUriMather = new PathMatcher();
	
	
	public GlobalDefaultInterceptor() {

		String contextPath = GlobalRuntimeContext.getContextPath();
		if(AppConfigs.invokeTokenCheckEnabled) {
			invoketokenCheckIgnoreUriMather.addUriPattern(contextPath, "/error");
		}
		for (String uri : AppConfigs.invokeTokenIgnoreUris) {
			invoketokenCheckIgnoreUriMather.addUriPattern(contextPath, uri);
		}
		
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		ThreadLocalContext.unset();
		ThreadLocalContext.set(ThreadLocalContext.REQUEST_CONTEXT_KEY, request);
		
		Enumeration<String> headerNames = request.getHeaderNames();
		String headerName;
		while(headerNames.hasMoreElements()) {
			headerName = headerNames.nextElement();
			CurrentRuntimeContext.addContextHeader(headerName,request.getHeader(headerName));
		}
		
	   //
		if(AppConfigs.invokeTokenCheckEnabled){	
			String uri = request.getRequestURI();
			if(!invoketokenCheckIgnoreUriMather.match(uri)){				
				String authCode = request.getHeader(CustomRequestHeaders.HEADER_INVOKE_TOKEN);
				try {					
					TokenGenerator.validate(authCode, true);
				} catch (MendmixBaseException e) {
					throw new MendmixBaseException(403, "invoke-" + e.getMessage());
				}
			}
		}
	
		if(handler instanceof HandlerMethod){
			HandlerMethod method = (HandlerMethod)handler;
			ApiMetadata  config = method.getMethod().getAnnotation(ApiMetadata.class);
			if(config != null){
				if(config.IntranetAccessOnly() && !WebUtils.isInternalRequest(request)){
					response.setStatus(403);
					if(log.isDebugEnabled()) {
						WebUtils.printRequest(request);
					}
					throw new ForbiddenAccessException();
				}
	
				//@ResponseBody and ResponseEntity的接口在postHandle addHeader不生效，因为会经过HttpMessageConverter
				if(config.responseKeep()){
					response.addHeader(CustomRequestHeaders.HEADER_RESP_KEEP, Boolean.TRUE.toString());
				}
			}
		}
	
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		ActionLogCollector.onResponseEnd(response.getStatus(), ex);
		ThreadLocalContext.unset();
	}
	
}
