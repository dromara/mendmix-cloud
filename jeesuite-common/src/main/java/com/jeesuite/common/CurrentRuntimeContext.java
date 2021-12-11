package com.jeesuite.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.exception.UnauthorizedException;
import com.jeesuite.common.model.AuthUser;

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
	
	

	public static void init(HttpServletRequest request, HttpServletResponse response){
		
		ThreadLocalContext.unset();
		
		ThreadLocalContext.set(ThreadLocalContext.REQUEST_KEY, request);
		ThreadLocalContext.set(ThreadLocalContext.RESPONSE_KEY, response);
		String tenantId = request.getHeader(CustomRequestHeaders.HEADER_TENANT_ID);
		if(tenantId != null)setTenantId(tenantId);
		String clientType = request.getHeader(CustomRequestHeaders.HEADER_CLIENT_TYPE);
		if(clientType != null)setClientType(clientType);
	}
	
	public static HttpServletRequest getRequest() {
		if(ThreadLocalContext.exists(ThreadLocalContext.REQUEST_KEY)){
			return ThreadLocalContext.get(ThreadLocalContext.REQUEST_KEY);
		}
		return null;
	}
	
	public static HttpServletResponse getResponse() {
		return ThreadLocalContext.get(ThreadLocalContext.RESPONSE_KEY);
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
			String headerString = request.getHeader(CustomRequestHeaders.HEADER_AUTH_USER);
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
			return getRequest().getHeader(CustomRequestHeaders.HEADER_INVOKER_APP_ID);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static String getRequestId(){
		try {			
			return getRequest().getHeader(CustomRequestHeaders.HEADER_REQUEST_ID);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static String getTenantId(boolean validate){
		String tenantId = ThreadLocalContext.getStringValue(ThreadLocalContext.TENANT_ID_KEY);
		if(tenantId == null){
			HttpServletRequest request = getRequest();
			if(request != null)tenantId = request.getHeader(CustomRequestHeaders.HEADER_TENANT_ID);
		}
		if(validate && StringUtils.isBlank(tenantId)){
			throw new JeesuiteBaseException(500,"无法识别租户信息");
		}
		return tenantId;
	}
	
	public static void setClientType(String clientType){
		if(StringUtils.isBlank(clientType))return;
		ThreadLocalContext.set(CustomRequestHeaders.HEADER_CLIENT_TYPE, clientType);
	}
	
	
	public static String getClientType() {
		String clientType = ThreadLocalContext.getStringValue(CustomRequestHeaders.HEADER_CLIENT_TYPE);
		if(clientType != null)return clientType;
		try {
			return getRequest().getHeader(CustomRequestHeaders.HEADER_CLIENT_TYPE);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static void setSystemId(String systemId){
		if(StringUtils.isBlank(systemId))return;
		ThreadLocalContext.set(CustomRequestHeaders.HEADER_SYSTEM_ID, systemId);
	}
	
	
	public static String getSystemId() {
		String systemId = ThreadLocalContext.getStringValue(CustomRequestHeaders.HEADER_SYSTEM_ID);
		if(systemId != null)return systemId;
		try {
			return getRequest().getHeader(CustomRequestHeaders.HEADER_SYSTEM_ID);
		} catch (Exception e) {
			return null;
		}
	}

}
