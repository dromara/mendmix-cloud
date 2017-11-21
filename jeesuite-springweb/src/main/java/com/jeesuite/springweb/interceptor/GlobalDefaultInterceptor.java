package com.jeesuite.springweb.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.jeesuite.springweb.WebConstants;
import com.jeesuite.springweb.annotation.ResponsePolicy;

public class GlobalDefaultInterceptor implements HandlerInterceptor {

	private static Logger log = LoggerFactory.getLogger(GlobalDefaultInterceptor.class);
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		if(handler instanceof HandlerMethod){
			HandlerMethod method = (HandlerMethod)handler;
			//TODO
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
		if(handler instanceof HandlerMethod){
			HandlerMethod method = (HandlerMethod)handler;
			ResponsePolicy  config = method.getMethod().getAnnotation(ResponsePolicy.class);
			if(config != null){
				response.addHeader(WebConstants.HEADER_RESP_POLICY, StringUtils.join(config.value(),','));
			}
		}
	}
	
}
