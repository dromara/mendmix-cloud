/*
 * Copyright 2016-2022 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dromara.mendmix.common.exception.UnauthorizedException;
import org.dromara.mendmix.common.guid.GUID;
import org.dromara.mendmix.common.model.AuthUser;

/**
 * 当前上下文 <br>
 * Class Name : CurrentRuntimeContext
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年08月21日
 */
public class CurrentRuntimeContext {

	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix");
	
	private static final String CURRENT_USER_KEY = "_ctx_currentUser_";
	private static final String CURRENT_TENANT_DS_KEY = "_ctx_currentTenantDsKey_";
	private static List<String> contextHeaders = Arrays.asList( 
			CustomRequestHeaders.HEADER_INVOKE_TOKEN,
			CustomRequestHeaders.HEADER_AUTH_USER,
			CustomRequestHeaders.HEADER_USER_ID,
			CustomRequestHeaders.HEADER_TENANT_ID,
			CustomRequestHeaders.HEADER_SYSTEM_ID,
			CustomRequestHeaders.HEADER_CLIENT_TYPE, 
			CustomRequestHeaders.HEADER_INVOKER_APP_ID, 
			CustomRequestHeaders.HEADER_REQUEST_ID ,
			CustomRequestHeaders.HEADER_TRACE_LOGGING,
			CustomRequestHeaders.ACCEPT_LANGUAGE
	);

	public static void addContextHeaders(Map<String, String> headers) {

		String headerVal;
		for (String headerName : contextHeaders) {
			if (!headers.containsKey(headerName))
				continue;
			headerVal = headers.get(headerName);
			if (headerVal != null) {
				if(CustomRequestHeaders.HEADER_AUTH_USER.equals(headerName)) {
					AuthUser user = AuthUser.decode(headerVal);
					if (user != null) {
						ThreadLocalContext.set(CustomRequestHeaders.HEADER_AUTH_USER, user);
					}
				}else {
					setContextVal(headerName, headerVal);
				}
			}
		}
	}

	public static void addContextHeader(String name,String value) {
		if(CustomRequestHeaders.HEADER_AUTH_USER.equals(name)) {
			AuthUser user = AuthUser.decode(value);
			if (user != null) {
				ThreadLocalContext.set(CustomRequestHeaders.HEADER_AUTH_USER, user);
			}
		}else if(contextHeaders.contains(name)){
			setContextVal(name, value);
		}
	}
	
	public static Map<String, Object> getAllContextVals(){
		Map<String, Object> map = new HashMap<>(contextHeaders.size());
		Object value;
		for (String key : contextHeaders) {
			if(CustomRequestHeaders.HEADER_AUTH_USER.equals(key))continue;
			if(ThreadLocalContext.exists(key)) {
				value = ThreadLocalContext.get(key);
				if(value != null)map.put(key, value);
			}else {
				value = getContextVal(key,false);
				if(value == null || StringUtils.isBlank(value.toString()))continue;
				map.put(key, value);
			}
		}
		//
		if(!map.containsKey(CURRENT_USER_KEY)) {
			AuthUser currentUser = getCurrentUser();
			if(currentUser != null) {
				map.put(CURRENT_USER_KEY, currentUser);
			}
		}
		//
		if(GlobalConstants.VIRTUAL_TENANT_ID.equals(map.get(CustomRequestHeaders.HEADER_TENANT_ID))) {
			map.remove(CustomRequestHeaders.HEADER_TENANT_ID);
		}
		
		return map;
	}

	public static Map<String, String> getContextHeaders() {
		Map<String, String> map = new HashMap<>(contextHeaders.size());
		String headerVal;
		for (String headerName : contextHeaders) {
			if(CustomRequestHeaders.HEADER_AUTH_USER.equals(headerName) && ThreadLocalContext.exists(headerName)) {
				headerVal = getCurrentUser().toEncodeString();
			}else {
				headerVal = getContextVal(headerName, false);
			}
			if(headerVal == null)continue;
			map.put(headerName, headerVal);
		}
		//
		if(!map.containsKey(CustomRequestHeaders.HEADER_REQUEST_ID)) {
			map.put(CustomRequestHeaders.HEADER_REQUEST_ID, GUID.uuid());
		}
		return map;
	}
	
	public static boolean isTraceLogging() {
		return Boolean.parseBoolean(getContextVal(CustomRequestHeaders.HEADER_TRACE_LOGGING, false));
	}

	public static void setTenantId(String tenantId) {
		setContextVal(CustomRequestHeaders.HEADER_TENANT_ID, tenantId);
	}

	public static String getCurrentUserId() {
		AuthUser currentUser = getCurrentUser();
		return currentUser == null ? null : StringUtils.defaultString(currentUser.getId(),currentUser.getName());
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

	public static void setRequestId(String requestId) {
		setContextVal(CustomRequestHeaders.HEADER_REQUEST_ID, requestId);
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
	
	public static void setBusinessUnitId(String businessUnitId){
		setContextVal(CustomRequestHeaders.HEADER_BUSINESS_UNIT_ID, businessUnitId);
	}
	
	public static String getBusinessUnitId(){	
		String buId = getContextVal(CustomRequestHeaders.HEADER_BUSINESS_UNIT_ID, false);
		return buId;
	}
	
	public static String timestamp() {
		String timestamp = getContextVal(CustomRequestHeaders.HEADER_TIMESTAMP, false);
		if(timestamp == null) {
			timestamp = String.valueOf(System.currentTimeMillis());
			setContextVal(CustomRequestHeaders.HEADER_TIMESTAMP, timestamp);
		}
		return timestamp;
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
	
	public static boolean isDebugMode() {
		return ThreadLocalContext.exists(GlobalConstants.DEBUG_TRACE_PARAM_NAME);
	}
	
	public static String getLanguage(boolean withCountry) {
		try {
			String language = getContextVal(CustomRequestHeaders.ACCEPT_LANGUAGE,false);
			if(language == null)return Locale.CHINA.getLanguage();
			language = StringUtils.split(language, ",;")[0];
			if(withCountry)return language;
			return StringUtils.split(language, GlobalConstants.MID_LINE)[0];
		} catch (Exception e) {
			return Locale.CHINA.getLanguage();
		}
	}
	
	public static Locale getLocale() {
		String language = getLanguage(true);
		if(language == null)return null;
		String[] parts = StringUtils.split(language, GlobalConstants.MID_LINE);
		return parts.length < 2 ? new Locale(parts[0]) : new Locale(parts[0], parts[1]);
	}
	
	public static void setTimeZone(String timeZone){
		setContextVal(CustomRequestHeaders.HEADER_TIME_ZONE, timeZone);
	}
	
	public static String getTimeZone(){
		return getContextVal(CustomRequestHeaders.HEADER_TIME_ZONE,false);
	}
	
	public static void setTenantDataSourceKey(String value) {
		setContextVal(CURRENT_TENANT_DS_KEY, value);
	}

	public static String getTenantDataSourceKey() {
		return getContextVal(CURRENT_TENANT_DS_KEY, false);
	}

	public static void setContextVal(String headerName, String value) {
		if (StringUtils.isBlank(value))
			return;
		ThreadLocalContext.set(headerName, value);
		if (logger.isTraceEnabled()) {
			logger.trace("set current context:[{}={}]", headerName, value);
		}
	}

	public static String getContextVal(String contexrName, boolean validate) {
		String value = ThreadLocalContext.getStringValue(contexrName);
		if (validate && StringUtils.isBlank(value)) {
			throw new MendmixBaseException(500, "无法获取上下文[" + contexrName + "]信息");
		}
		return value;
	}
	
	public static boolean hasContextVal(String contextName) {
		return ThreadLocalContext.exists(contextName) || getContextVal(contextName, false) != null;
	}

	@SuppressWarnings("unchecked")
	private static <V> V getContextVal(String contextName, Class<V> vClass, boolean validate) {
		String value = getContextVal(contextName, validate);
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
