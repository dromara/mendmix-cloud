package com.jeesuite.springweb.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.jeesuite.springweb.annotation.CorsEnabled;

public class CorsEnableInterceptor implements HandlerInterceptor{

	private String allowOrigin = "*";
	private String allowMethods = "GET,POST,OPTIONS";
	private String allowHeaders = "Content-Type, Content-Length, X-Requested-With";
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		
		if(handler instanceof HandlerMethod){
			HandlerMethod method = (HandlerMethod)handler;
			CorsEnabled  config = method.getMethod().getAnnotation(CorsEnabled.class);
			if(config != null){
				response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigin);
				response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowMethods);
				response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
			}
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {}

}
