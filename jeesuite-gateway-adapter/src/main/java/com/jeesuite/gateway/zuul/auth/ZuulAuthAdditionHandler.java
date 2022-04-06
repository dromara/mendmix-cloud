package com.jeesuite.gateway.zuul.auth;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.GlobalConstants;
import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.ThreadLocalContext;
import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.common.util.IpUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.common.util.WebUtils;
import com.jeesuite.gateway.CurrentSystemHolder;
import com.jeesuite.gateway.FilterConstants;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.gateway.model.BizSystemPortal;
import com.jeesuite.gateway.zuul.filter.pre.SignatureRequestHandler;
import com.jeesuite.logging.integrate.ActionLogCollector;
import com.jeesuite.security.AuthAdditionHandler;
import com.jeesuite.security.model.UserSession;

/**
 * 
 * 
 * <br>
 * Class Name   : GlobalAdditionHandler
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Jan 23, 2022
 */
public class ZuulAuthAdditionHandler implements AuthAdditionHandler {
	
	private static boolean openApiEnabled;

	private boolean actionLogEnabled = ResourceUtils.getBoolean("jeesuite.actionLog.collector.enabled", false);
	
	private boolean ignoreReadMethodLog = ResourceUtils.getBoolean("jeesuite.actionLog.readMethod.ignore", true);
	
	private List<String> anonymousIpWhilelist = ResourceUtils.getList("jeesuite.acl.anonymous-ip-whilelist");
	
	public static void setOpenApiEnabled(boolean openApiEnabled) {
		ZuulAuthAdditionHandler.openApiEnabled = openApiEnabled;
	}

	@Override
	public void beforeAuthentication(HttpServletRequest request, HttpServletResponse response) {
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
	public boolean customAuthentication(HttpServletRequest request) {
		boolean pass = openApiEnabled && request.getHeader(SignatureRequestHandler.X_SIGN_HEADER) != null;
		if(!pass) {
			pass = isIpWhilelistAccess(request);
		}
		if(!pass) {
			pass = isInternalTrustedAccess(request);
		}
		if(!pass) {
			pass = isCrossClusterTrustedAccess(request);
		}
		return pass;
	}

	@Override
	public void afterAuthentication(UserSession userSession) {
		if(!actionLogEnabled)return;
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		BizSystemModule module = CurrentSystemHolder.getModule(currentRouteName(request.getRequestURI()));
		
		ApiInfo apiInfo = module.getApiInfo(request.getRequestURI());
		boolean logging = apiInfo != null ? apiInfo.isActionLog() : true;
		if(logging) {
			logging = !ignoreReadMethodLog || !request.getMethod().equals(RequestMethod.GET.name());
		}
		if(logging){
			ActionLogCollector.onRequestStart(request).apiMeta(apiInfo);
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
	
	/**
	 * 匿名访问白名单
	 * @param request
	 * @return
	 */
	private boolean isIpWhilelistAccess(HttpServletRequest request) {

		if(!anonymousIpWhilelist.isEmpty()) {
			String clientIp = IpUtils.getIpAddr(request);
			if(anonymousIpWhilelist.contains(clientIp))return true;
		}
		
		return false;
	}
	
	private boolean isInternalTrustedAccess(HttpServletRequest request) {
		String header = request.getHeader(CustomRequestHeaders.HEADER_IGNORE_AUTH);
		String header1 = request.getHeader(CustomRequestHeaders.HEADER_INTERNAL_REQUEST);
		if(Boolean.parseBoolean(header) && Boolean.parseBoolean(header1)) {
			if(validateInvokeToken(request)) {
				ThreadLocalContext.set(FilterConstants.CONTEXT_TRUSTED_REQUEST, Boolean.TRUE);
				return true;
			}
		}
		return false;
	}
	
	private boolean isCrossClusterTrustedAccess(HttpServletRequest request) {
		boolean crossCluster = false;
		try {
			String clusterName = request.getHeader(CustomRequestHeaders.HEADER_CLUSTER_ID);
			if(StringUtils.isNotBlank(clusterName)) {
				if(validateInvokeToken(request)) {
					ThreadLocalContext.set(FilterConstants.CONTEXT_TRUSTED_REQUEST, Boolean.TRUE);
					crossCluster = true;
				}
			}
		} catch (Exception e) {}
		
		return crossCluster;
	}
	
	private boolean validateInvokeToken(HttpServletRequest request) {
		String token = request.getHeader(CustomRequestHeaders.HEADER_INVOKE_TOKEN);
		if(StringUtils.isBlank(token))return false;
		try {
			TokenGenerator.validate(token, true);
			return true;
		} catch (Exception e) {}
		
		return false;
	}
}
