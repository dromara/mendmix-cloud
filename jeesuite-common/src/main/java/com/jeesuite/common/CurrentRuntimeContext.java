package com.jeesuite.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.exception.UnauthorizedException;
import com.jeesuite.common.model.AuthUser;

/**
 * 当前上下文 <br>
 * Class Name : CurrentRuntimeContext
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年08月21日
 */
public class CurrentRuntimeContext {

	private final static Logger logger = LoggerFactory.getLogger("com.jeesuite");
	
	private static List<String> contextHeaders = Arrays.asList( 
			CustomRequestHeaders.HEADER_FROWARDED_FOR,
			CustomRequestHeaders.HEADER_TENANT_ID,
			CustomRequestHeaders.HEADER_CLIENT_TYPE, 
			CustomRequestHeaders.HEADER_PLATFORM_TYPE,
			CustomRequestHeaders.HEADER_INVOKER_APP_ID, 
			CustomRequestHeaders.HEADER_REQUEST_ID 
	);

	public static void addContextHeaders(Map<String, String> headers) {
		
		ThreadLocalContext.unset();
		String headerVal;
		for (String headerName : contextHeaders) {
			if (!headers.containsKey(headerName))
				continue;
			headerVal = headers.get(headerName);
			if (headerVal != null) {
				setContextVal(headerName, headerVal);
			}
		}
        //
		if (headers.containsKey(CustomRequestHeaders.HEADER_AUTH_USER)) {
			headerVal = headers.get(CustomRequestHeaders.HEADER_AUTH_USER);
			AuthUser user = AuthUser.decode(headerVal);
			if (user != null) {
				ThreadLocalContext.set(CustomRequestHeaders.HEADER_AUTH_USER, user);
			}
		}
	}

	public static void addContextHeader(String name,String value) {
		if(contextHeaders.contains(name)){
			setContextVal(name, value);
		}else if(CustomRequestHeaders.HEADER_AUTH_USER.equals(name)) {
			AuthUser user = AuthUser.decode(value);
			if (user != null) {
				ThreadLocalContext.set(CustomRequestHeaders.HEADER_AUTH_USER, user);
			}
		}
	}

	public static Map<String, String> getCustomHeaders() {
		Map<String, String> map = new HashMap<>();
		String headerVal;
		for (String headerName : contextHeaders) {
			headerVal = getContextVal(headerName, false);
			if(headerVal == null)continue;
			map.put(headerName, headerVal);
		}
		return map;
	}

	public static void setTenantId(String tenantId) {
		setContextVal(CustomRequestHeaders.HEADER_TENANT_ID, tenantId);
	}

	public static String getCurrentUserId() {
		AuthUser currentUser = getCurrentUser();
		return currentUser == null ? null : currentUser.getId();
	}

	public static AuthUser getCurrentUser() {
		AuthUser user = ThreadLocalContext.get(CustomRequestHeaders.HEADER_AUTH_USER);
		return user;
	}

	public static void setAuthUser(AuthUser user) {
		if (user == null) {
			return;
		} else {
			ThreadLocalContext.set(CustomRequestHeaders.HEADER_AUTH_USER, user);
		}
	}

	/**
	 * 获取登录信息，未登录抛出异常
	 * 
	 * @return
	 */
	public static AuthUser getAndValidateCurrentUser() {
		AuthUser user = getCurrentUser();
		if (user == null)
			throw new UnauthorizedException();
		return user;
	}

	public static String getInvokeAppId() {
		return getContextVal(CustomRequestHeaders.HEADER_INVOKER_APP_ID, false);
	}

	public static String getRequestId() {
		return getContextVal(CustomRequestHeaders.HEADER_REQUEST_ID, false);
	}

	public static String getTenantId() {
		return getContextVal(CustomRequestHeaders.HEADER_TENANT_ID, false);
	}

	public static String getTenantId(boolean validate) {
		return getContextVal(CustomRequestHeaders.HEADER_TENANT_ID, validate);
	}

	public static void setPlatformType(String platformType) {
		setContextVal(CustomRequestHeaders.HEADER_PLATFORM_TYPE, platformType);
	}

	public static String getPlatformType() {
		return getContextVal(CustomRequestHeaders.HEADER_PLATFORM_TYPE, false);
	}

	public static void setClientType(String clientType) {
		setContextVal(CustomRequestHeaders.HEADER_CLIENT_TYPE, clientType);
	}

	public static String getClientType() {
		return getContextVal(CustomRequestHeaders.HEADER_CLIENT_TYPE, false);
	}

	public static void setSystemId(String systemId) {
		setContextVal(CustomRequestHeaders.HEADER_SYSTEM_ID, systemId);
	}

	public static String getSystemId() {
		return getContextVal(CustomRequestHeaders.HEADER_SYSTEM_ID, false);
	}

	public static <T> T getSystemId(Class<T> clazz) {
		return getContextVal(CustomRequestHeaders.HEADER_SYSTEM_ID, clazz, false);
	}

	public static void setIgnoreTenant(Boolean ignore) {
		setContextVal(CustomRequestHeaders.HEADER_IGNORE_TENANT, ignore.toString());
	}

	public static boolean getIgnoreTenant() {
		return Boolean.parseBoolean(getContextVal(CustomRequestHeaders.HEADER_IGNORE_TENANT, false));
	}

	private static void setContextVal(String headerName, String value) {
		if (StringUtils.isBlank(value))
			return;
		ThreadLocalContext.set(headerName, value);
		if (logger.isTraceEnabled()) {
			logger.trace("set current context:[{}={}]", headerName, value);
		}
	}

	private static String getContextVal(String headerName, boolean validate) {
		String value = ThreadLocalContext.getStringValue(headerName);
		if (validate && StringUtils.isBlank(value)) {
			throw new JeesuiteBaseException(500, "无法获取上下文[" + headerName + "]信息");
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	private static <V> V getContextVal(String headerName, Class<V> vClass, boolean validate) {
		String value = getContextVal(headerName, validate);
		if (StringUtils.isBlank(value))
			return null;
		Object newVal = value;
		if (vClass == Integer.class) {
			newVal = Integer.parseInt(value);
		} else if (vClass == Long.class) {
			newVal = Long.parseLong(value);
		}
		return (V) newVal;
	}

}
