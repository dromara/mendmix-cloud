package com.jeesuite.zuul.filter.global;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.GlobalConstants;
import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.WebUtils;
import com.jeesuite.logging.integrate.RequestLogCollector;
import com.jeesuite.security.AuthAdditionHandler;
import com.jeesuite.security.model.UserSession;
import com.jeesuite.zuul.CurrentSystemHolder;
import com.jeesuite.zuul.model.BizSystemModule;
import com.jeesuite.zuul.model.BizSystemPortal;

/**
 * 
 * 
 * <br>
 * Class Name   : GlobalAuthAdditionHandler
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Jan 23, 2022
 */
public class GlobalAdditionHandler implements AuthAdditionHandler {

	private boolean actionLogEnabled = ResourceUtils.getBoolean("jeesuite.actionLog.collector.enabled", false);
	
	private boolean ignoreReadMethodLog = ResourceUtils.getBoolean("jeesuite.actionLog.readMethod.ignore", true);
	
	@Override
	public void beforeAuthorization(HttpServletRequest request, HttpServletResponse response) {
		//
		
		//客户端标识
		String clientType = null;
		String platformType = null; 
		String tenantId = null; //域名识别租户
		String domain = WebUtils.getOriginDomain(request);
		BizSystemPortal portal = CurrentSystemHolder.getSystemPortal(domain);
		if(portal != null) {
			clientType = portal.getClientType();
			platformType = portal.getCode();
			tenantId = portal.getTenantId();
		}
		
		CurrentRuntimeContext.setTenantId(tenantId);
		//
		String headerValue = request.getHeader(CustomRequestHeaders.HEADER_CLIENT_TYPE);
		CurrentRuntimeContext.setClientType(StringUtils.defaultIfBlank(headerValue, clientType));
		//
		headerValue = request.getHeader(CustomRequestHeaders.HEADER_PLATFORM_TYPE);
		CurrentRuntimeContext.setPlatformType(StringUtils.defaultIfBlank(headerValue, platformType));
	}

	@Override
	public void afterAuthorization(UserSession userSession) {
		if(!actionLogEnabled)return;
		HttpServletRequest request = CurrentRuntimeContext.getRequest();
		BizSystemModule module = CurrentSystemHolder.getModule(currentRouteName(request.getRequestURI()));
		
		ApiInfo apiInfo = module.getApiInfo(request.getRequestURI());
		boolean logging = apiInfo != null ? apiInfo.isActionLog() : true;
		if(logging) {
			logging = !ignoreReadMethodLog || !request.getMethod().equals(RequestMethod.GET.name());
		}
		if(logging){
			RequestLogCollector.onRequestStart(request).apiMeta(apiInfo);
		}
	
	}
	
	private String currentRouteName(String uri) {
		String contextPath = GlobalRuntimeContext.getContextPath();
		int indexOf = StringUtils.indexOf(uri, GlobalConstants.PATH_SEPARATOR, contextPath.length());
		uri = uri.substring(indexOf + 1);
		
		List<String> routeNames = CurrentSystemHolder.getRouteNames();
		for (String routeName : routeNames) {
			if(uri.startsWith(routeName + GlobalConstants.PATH_SEPARATOR)) {
				return routeName;
			}
		}
		return GlobalRuntimeContext.APPID;
	}

}
