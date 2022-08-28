/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.security;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.GlobalConstants;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.model.AuthUser;
import com.mendmix.security.context.ReactiveRequestContextAdapter;
import com.mendmix.security.context.ServletRequestContextAdapter;
import com.mendmix.security.model.UserSession;

/**
 * session管理器
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年12月4日
 */
public class SecuritySessionManager {

	private static final String PERMISSION_KEY_PREFIX = "permission:";
	private static final String API_ITEM_KEY = "api_%s_%s";

	private static String cacheName = "session";

	private volatile String cookieDomain;
	private String headerTokenName;
	private String sessionIdName;
	private boolean setCookie;
	private boolean keepCookie;
	private int sessionExpireIn = 0;
	private long sessionExpireInMills = 0;
	
	private SecurityStorageManager storageManager;
	
	private RequestContextAdapter requestContextAdapter;

	public SecuritySessionManager(SecurityDecisionProvider decisionProvider,SecurityStorageManager storageManager) {
		this.storageManager = storageManager;
		this.cookieDomain = decisionProvider.cookieDomain();
		this.sessionIdName = decisionProvider.sessionIdName();
		this.headerTokenName = decisionProvider.headerTokenName();
		this.keepCookie = decisionProvider.keepCookie();
		this.sessionExpireIn = decisionProvider.sessionExpireIn();
		this.sessionExpireInMills = this.sessionExpireIn * 1000;
		//
		this.storageManager.addCahe(cacheName, this.sessionExpireIn);
		//
		if(decisionProvider.isServletType()) {
			requestContextAdapter = new ServletRequestContextAdapter();
		}else {
			requestContextAdapter = new ReactiveRequestContextAdapter();
		}
	}

	public UserSession getLoginSession(String sessionId) {
		if (StringUtils.isBlank(sessionId))
			return null;
		return storageManager.getCache(cacheName).getObject(sessionId);
	}

	public UserSession getSession() {
		return getSession(setCookie);
	}

	public UserSession getSession(boolean createIfAbsent) {
		String sessionId = getSessionId();
		
		UserSession session = null;
		if (StringUtils.isNotBlank(sessionId)) {
			session = getLoginSession(sessionId);
		} 
		if (createIfAbsent && session == null) {
			session = UserSession.create();
			if(sessionId != null && GlobalRuntimeContext.isDevEnv()) {
				session.setSessionId(sessionId);
			}
			int expire = keepCookie ? sessionExpireIn : -1;
			requestContextAdapter.addCookie(cookieDomain, cookieDomain, session.getSessionId(), expire);
			//
			storageLoginSession(session);
		}

		return session;

	}

	public UserSession getLoginSessionByUserId(AuthUser authUser) {
		String key = buildUserSessionUniqueKey(authUser);
		String sessionId = storageManager.getCache(cacheName).getString(key);
		if (StringUtils.isBlank(sessionId))
			return null;
		return getLoginSession(sessionId);
	}

	public void storageLoginSession(UserSession session) {
		String key = session.getSessionId();
		if (!session.isAnonymous()) {
			session.setExpiredAt(System.currentTimeMillis() + this.sessionExpireInMills);
			String uniquekey = buildUserSessionUniqueKey(session.getUser());
			storageManager.getCache(cacheName).setString(uniquekey, session.getSessionId());
		}
		storageManager.getCache(cacheName).setObject(key, session);
	}
	

	public void removeLoginSession(String sessionId) {
		String key = sessionId;
		UserSession session = getLoginSession(sessionId);
		if (session != null && !session.isAnonymous()) {
			storageManager.getCache(cacheName).remove(key);
			key = buildUserSessionUniqueKey(session.getUser());
			storageManager.getCache(cacheName).remove(key);
		}
	}
	
	public void updateUserPermissions(UserSession session, List<String> permissions) {
		if(permissions == null)return;
		String key = PERMISSION_KEY_PREFIX + session.getSessionId();
		String field = String.format(API_ITEM_KEY, StringUtils.trimToEmpty(session.getSystemId()),StringUtils.trimToEmpty(session.getTenanId()));
		storageManager.getCache(cacheName).setMapValue(key, field, permissions);
	}
	
	public List<String> getUserPermissions(UserSession session){
		String key = PERMISSION_KEY_PREFIX + session.getSessionId();
		String field = String.format(API_ITEM_KEY, StringUtils.trimToEmpty(session.getSystemId()),StringUtils.trimToEmpty(session.getTenanId()));
		return storageManager.getCache(cacheName).getMapValue(key, field);
	}
	
	public long getUpdateTime(UserSession session) {
		return session.getExpiredAt() - this.sessionExpireInMills;
	}

	public void setSessionAttribute(String name, Object object) {
		String sessionId = getSessionId();
		storageManager.getCache(cacheName).setMapValue(sessionId, name, object);
	}

	public <T> T getSessionAttribute(String name) {
		String sessionId = getSessionId();
		return storageManager.getCache(cacheName).getMapValue(sessionId, name);
	}


	public String getSessionId() {
		String sessionId = requestContextAdapter.getHeader(headerTokenName);
		if (StringUtils.isNotBlank(sessionId) && sessionId.length() >= 32) {
			return sessionId;
		}
		sessionId = requestContextAdapter.getCookie(sessionIdName);
		return sessionId;
	}

	public String destroySessionAndCookies() {
		String sessionId = getSessionId();
		if (StringUtils.isNotBlank(sessionId)) {
			removeLoginSession(sessionId);
			//
			requestContextAdapter.addCookie(cookieDomain, cookieDomain, StringUtils.EMPTY, 0);
		}
		return sessionId;
	}
	
	private static String buildUserSessionUniqueKey(AuthUser authUser) {
		StringBuilder builder = new StringBuilder();
		builder.append(StringUtils.defaultString(authUser.getId(), authUser.getName())).append(GlobalConstants.COLON);
		if(CurrentRuntimeContext.getSystemId() != null) {
			builder.append(CurrentRuntimeContext.getSystemId());
		}
		String clientType = CurrentRuntimeContext.getClientType();
		if(StringUtils.isNotBlank(clientType)) {
			builder.append(GlobalConstants.UNDER_LINE).append(clientType);
		}
		return builder.toString();
	}

}
