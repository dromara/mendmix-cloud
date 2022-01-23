package com.jeesuite.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private final static Logger logger = LoggerFactory.getLogger("com.jeesuite");

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
		setContextVal(CustomRequestHeaders.HEADER_TENANT_ID, tenantId);
	}
	
	public static String getCurrentUserId(){
		AuthUser currentUser = getCurrentUser();
		return currentUser == null ? null : currentUser.getId();
	}

	public static AuthUser getCurrentUser(){
		AuthUser user = ThreadLocalContext.get(CustomRequestHeaders.HEADER_AUTH_USER);
		if(user == null){
			HttpServletRequest request = getRequest();
			if(request == null)return null;
			String headerString = request.getHeader(CustomRequestHeaders.HEADER_AUTH_USER);
			user = AuthUser.decode(headerString);
			if(user != null){
				ThreadLocalContext.set(CustomRequestHeaders.HEADER_AUTH_USER, user);
			}
		}
		return user;
	}
	
	public static void setAuthUser(AuthUser user){
		if(user == null){
			return;
		}else{
			ThreadLocalContext.set(CustomRequestHeaders.HEADER_AUTH_USER, user);
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
	
	public static String getTenantId(){
		return getContextVal(CustomRequestHeaders.HEADER_TENANT_ID, false);
	}
	
	public static String getTenantId(boolean validate){
		return getContextVal(CustomRequestHeaders.HEADER_TENANT_ID, validate);
	}
	
	public static void setPlatformType(String platformType){
		setContextVal(CustomRequestHeaders.HEADER_PLATFORM_TYPE, platformType);
	}
	
	
	public static String getPlatformType() {
		return getContextVal(CustomRequestHeaders.HEADER_PLATFORM_TYPE, false);
	}
	
	public static void setClientType(String clientType){
		setContextVal(CustomRequestHeaders.HEADER_CLIENT_TYPE, clientType);
	}
	
	
	public static String getClientType() {
		return getContextVal(CustomRequestHeaders.HEADER_CLIENT_TYPE, false);
	}
	
	public static void setSystemId(String systemId){
		setContextVal(CustomRequestHeaders.HEADER_SYSTEM_ID, systemId);
	}
	
	
	public static String getSystemId() {
		return getContextVal(CustomRequestHeaders.HEADER_SYSTEM_ID, false);
	}
	
	public static <T> T getSystemId(Class<T> clazz) {
		return getContextVal(CustomRequestHeaders.HEADER_SYSTEM_ID,clazz, false);
	}
	
	public static void setIgnoreTenant(Boolean ignore){
		setContextVal(CustomRequestHeaders.HEADER_IGNORE_TENANT, ignore.toString());
	}
	
	
	public static boolean getIgnoreTenant() {
		return Boolean.parseBoolean(getContextVal(CustomRequestHeaders.HEADER_IGNORE_TENANT, false));
	}
	
	private static void setContextVal(String headerName,String value) {
		if(StringUtils.isBlank(value))return;
		ThreadLocalContext.set(headerName, value);
		if(logger.isTraceEnabled()) {
			logger.trace("set current context:[{}={}]",headerName,value);
		}
	}
	
	private static String getContextVal(String headerName,boolean validate){
		String value = ThreadLocalContext.getStringValue(headerName);
		if(value == null){
			HttpServletRequest request = getRequest();
			if(request != null)value = getRequest().getHeader(headerName);
		}
		if(validate && StringUtils.isBlank(value)){
			throw new JeesuiteBaseException(500,"无法获取上下文["+headerName+"]信息");
		}
		return value;
	}
	
	private static <V> V getContextVal(String headerName,Class<V> vClass,boolean validate){
		String value = getContextVal(headerName, validate);
		if(StringUtils.isBlank(value))return null;
		Object newVal = value;
		if(vClass == Integer.class) {
			newVal = Integer.parseInt(value);
		}else if(vClass == Long.class) {
			newVal = Long.parseLong(value);
		}	
		return (V) newVal;
	}

}
