package com.jeesuite.springweb.interceptor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.ThreadLocalContext;
import com.jeesuite.common.exception.ForbiddenAccessException;
import com.jeesuite.common.util.PathMatcher;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.common.util.WebUtils;
import com.jeesuite.springweb.annotation.ApiMetadata;
import com.jeesuite.springweb.logging.RequestLogCollector;

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

	private static Logger log = LoggerFactory.getLogger("com.jeesuite.springweb");
	
	private boolean isLocalEnv = "local".equals(GlobalRuntimeContext.ENV);
	//调用token检查
	private boolean authTokenCheckEnabled = ResourceUtils.getBoolean("request.authtoken.enabled", !isLocalEnv);
	//行为日志采集
	private boolean requestLogEnabled = ResourceUtils.getBoolean("request.log.enabled", false);
	//
	private boolean requestLogGetIngore = ResourceUtils.getBoolean("request.log.getMethod.ignore", true);

	private PathMatcher authtokenCheckIgnoreUriMather = new PathMatcher();
	
	
	public GlobalDefaultInterceptor() {
		String contextPath = GlobalRuntimeContext.getContextPath();
		if(authTokenCheckEnabled) {
			authtokenCheckIgnoreUriMather.addUriPattern(contextPath, "/error");
		}
		List<String> ignoreUris = ResourceUtils.getList("request.authtoken.ignore-uris");
		for (String uri : ignoreUris) {
			authtokenCheckIgnoreUriMather.addUriPattern(contextPath, uri);
		}
		
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		CurrentRuntimeContext.init(request, response);
		//校验授权
		if(authTokenCheckEnabled){	
			String uri = request.getRequestURI();
			if(!authtokenCheckIgnoreUriMather.match(uri)){				
				String authCode = request.getHeader(CustomRequestHeaders.HEADER_AUTH_TOKEN);
				TokenGenerator.validate(authCode, true);
			}
		}
	
		if(handler instanceof HandlerMethod){
			HandlerMethod method = (HandlerMethod)handler;
			ApiMetadata  config = method.getMethod().getAnnotation(ApiMetadata.class);
			if(config != null){

				if(!isLocalEnv && config.IntranetAccessOnly() && !WebUtils.isInternalRequest(request)){
					response.setStatus(403);
					throw new ForbiddenAccessException();
				}
	
				//@ResponseBody and ResponseEntity的接口在postHandle addHeader不生效，因为会经过HttpMessageConverter
				if(config.responseKeep()){
					response.addHeader(CustomRequestHeaders.HEADER_RESP_KEEP, Boolean.TRUE.toString());
				}
			}
			
			//行为日志
			if(requestLogEnabled){
				if((config == null && (!requestLogGetIngore || !request.getMethod().equals(RequestMethod.GET.name())))
						|| config.actionLog()){
					RequestLogCollector.onRequestStart(request);
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
		RequestLogCollector.onResponseEnd(response, ex);
		ThreadLocalContext.unset();
	}
	
}
