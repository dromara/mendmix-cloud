package com.jeesuite.springweb;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.ThreadLocalContext;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.springweb.exception.UnauthorizedException;

/**
 * 当前上下文
 * <br>
 * Class Name   : CurrentRuntimeContext
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年08月21日
 */
public class CurrentRuntimeContext {
	
	public static final String SERVICE_NAME;
	public static final String APPID;
	public static final String ENV;
	
	static {
		ENV = ResourceUtils.getAnyProperty("jeesuite.configcenter.profile","spring.profiles.active");
		SERVICE_NAME = ResourceUtils.getAnyProperty("spring.application.name","jeesuite.configcenter.appName");
		if(StringUtils.isBlank(SERVICE_NAME)){
			throw new IllegalArgumentException("config Property[spring.application.name] is required");
		}
		String[] strings = StringUtils.split(SERVICE_NAME, "-");
		if(strings.length == 3) {
			APPID = SERVICE_NAME.replace("-svc", "");
		} else {
			APPID = strings[0];
		}
	}
	
	private static final String REQUEST_KEY = "_ctx_request_";
	private static final String RESPONSE_KEY = "_ctx_response_";
	
	public static void init(HttpServletRequest request, HttpServletResponse response){
		
		ThreadLocalContext.unset();
		
		ThreadLocalContext.set(REQUEST_KEY, request);
		ThreadLocalContext.set(RESPONSE_KEY, response);
		String tenantId = request.getHeader(WebConstants.HEADER_TENANT_ID);
		if(tenantId != null)setTenantId(tenantId);
	}
	
	public static HttpServletRequest getRequest() {
		if(ThreadLocalContext.exists(REQUEST_KEY)){
			return ThreadLocalContext.get(REQUEST_KEY);
		}
		if(RequestContextHolder.getRequestAttributes() != null){
			return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		}
		return null;
	}
	
	public static HttpServletResponse getResponse() {
		return ThreadLocalContext.get(RESPONSE_KEY);
	}
	
	public static void setTenantId(String tenantId){
		if(StringUtils.isBlank(tenantId))return;
		ThreadLocalContext.set(ThreadLocalContext.TENANT_ID_KEY, tenantId);
	}
	

	public static AuthUser getCurrentUser(){
		AuthUser user = ThreadLocalContext.get(ThreadLocalContext.CURRENT_USER_KEY);
		if(user == null){
			HttpServletRequest request = getRequest();
			if(request == null)return null;
			String headerString = request.getHeader(WebConstants.HEADER_AUTH_USER);
			user = AuthUser.decode(headerString);
			if(user != null){
				ThreadLocalContext.set(ThreadLocalContext.CURRENT_USER_KEY, user);
			}
		}
		return user;
	}
	
	public static void setAuthUser(AuthUser user){
		if(user == null){
			return;
		}else{
			ThreadLocalContext.set(ThreadLocalContext.CURRENT_USER_KEY, user);
		}
	}

	/**
	 * 获取登录信息，未登录抛出异常
	 * @return
	 */
	public static AuthUser getAndValidateCurrentUser(){
		AuthUser user = getCurrentUser();
		if(user == null)throw new UnauthorizedException();
		return user;
	}
	
	
	public static String getInvokeAppId(){
		try {			
			return getRequest().getHeader(WebConstants.HEADER_INVOKER_APP_ID);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static String getRequestId(){
		try {			
			return getRequest().getHeader(WebConstants.HEADER_REQUEST_ID);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static String getTenantId(boolean validate){
		String tenantId = ThreadLocalContext.getStringValue(ThreadLocalContext.TENANT_ID_KEY);
		if(tenantId == null){
			tenantId = getRequest().getHeader(WebConstants.HEADER_REQUEST_ID);
		}
		if(validate && StringUtils.isBlank(tenantId)){
			throw new JeesuiteBaseException(500,"无法识别租户信息");
		}
		return tenantId;
	}
	
	

}
