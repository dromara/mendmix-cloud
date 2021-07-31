package com.jeesuite.springweb.interceptor;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.jeesuite.common.ThreadLocalContext;
import com.jeesuite.common.WebConstants;
import com.jeesuite.common.util.PathMatcher;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.common.util.WebUtils;
import com.jeesuite.springweb.CurrentRuntimeContext;
import com.jeesuite.springweb.annotation.ApiMetadata;
import com.jeesuite.springweb.exception.ForbiddenAccessException;
import com.jeesuite.springweb.logging.BehaviorLogCollector;

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
	
	private boolean isLocalEnv = "local".equals(CurrentRuntimeContext.ENV);
	private boolean authTokenCheckDisabled = ResourceUtils.getBoolean("authtoken.check.disabled", isLocalEnv);
	private static boolean behaviorlogEnabled = ResourceUtils.getBoolean("behaviorlog.collector.enabled", false);
	private static List<String> actionLogMethoads = Arrays.asList(HttpMethod.POST.name(),HttpMethod.DELETE.name(),HttpMethod.PUT.name());

	private PathMatcher authtokenCheckIgnoreUriMather = new PathMatcher(StringUtils.EMPTY,ResourceUtils.getProperty("authtoken.check.ignore.uris"));
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		CurrentRuntimeContext.init(request, response);
		//校验授权
		if(!authTokenCheckDisabled){	
			String uri = request.getRequestURI();
			if(!authtokenCheckIgnoreUriMather.match(uri)){				
				String authCode = request.getHeader(WebConstants.HEADER_AUTH_TOKEN);
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
					response.addHeader(WebConstants.HEADER_RESP_KEEP, Boolean.TRUE.toString());
				}
				//行为日志
				if(behaviorlogEnabled){
					if((config == null && (actionLogMethoads.contains(request.getMethod()))) 
							|| (config != null && config.actionLog())){
						BehaviorLogCollector.onRequestStart(request);
					}
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
		if(behaviorlogEnabled){			
			BehaviorLogCollector.onResponseEnd(response, ex);
		}
		ThreadLocalContext.unset();
	}
	
}
