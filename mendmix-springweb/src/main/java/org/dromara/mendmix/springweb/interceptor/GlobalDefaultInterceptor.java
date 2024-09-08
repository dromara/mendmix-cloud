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
package org.dromara.mendmix.springweb.interceptor;

import java.util.Collection;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.annotation.ApiMetadata;
import org.dromara.mendmix.common.exception.ForbiddenAccessException;
import org.dromara.mendmix.common.util.PathMatcher;
import org.dromara.mendmix.common.util.TokenGenerator;
import org.dromara.mendmix.common.util.WebUtils;
import org.dromara.mendmix.logging.reqlog.RequestLogCollector;
import org.dromara.mendmix.springweb.AppConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

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

	private static Logger log = LoggerFactory.getLogger("org.dromara.mendmix.springweb");

	private PathMatcher invoketokenCheckIgnoreUriMather = new PathMatcher();
	
	
	public GlobalDefaultInterceptor() {

		String contextPath = GlobalContext.getContextPath();
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
		ThreadLocalContext.set(ThreadLocalContext.REQUEST_KEY, request);
		
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
				if(config.intranetAccess() && !WebUtils.isInternalRequest(request)){
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
		if(ex == null) {
			//全局拦截器已处理的异常
			ex = ThreadLocalContext.get(GlobalConstants.CONTEXT_EXCEPTION);
		}
		RequestLogCollector.onResponseEnd(response, ex);
		if(ThreadLocalContext.exists(GlobalConstants.DEBUG_TRACE_PARAM_NAME)) {
			Collection<String> headerNames = response.getHeaderNames();
			System.out.println("\n============Response Begin==============");
			for (String headerName : headerNames) {
				System.out.println(String.format("header[%s] = %s", headerName,response.getHeader(headerName)));
			}
			System.out.println("============Response End==============\n");
		}
		if(log.isDebugEnabled() && ThreadLocalContext.exists(ThreadLocalContext.REQUEST_TIME_KEY)) {
			long startTime = ThreadLocalContext.get(ThreadLocalContext.REQUEST_TIME_KEY);
			log.debug(">>request[{}] end,use time:{} ms",request.getRequestURI(),System.currentTimeMillis() - startTime);
		}
		ThreadLocalContext.unset();
	}
	
}
