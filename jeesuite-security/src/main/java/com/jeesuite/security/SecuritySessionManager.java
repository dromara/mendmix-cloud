package com.jeesuite.security;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.GlobalConstants;
import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.security.context.ReactiveRequestContextAdapter;
import com.jeesuite.security.context.ServletRequestContextAdapter;
import com.jeesuite.security.model.UserSession;

/**
 * session管理器
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年12月4日
 */
public class SecuritySessionManager {

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
		String platformType = CurrentRuntimeContext.getPlatformType();
		if(StringUtils.isNotBlank(platformType)) {
			builder.append(GlobalConstants.UNDER_LINE).append(platformType);
		}
		String clientType = CurrentRuntimeContext.getClientType();
		if(StringUtils.isNotBlank(clientType)) {
			builder.append(GlobalConstants.UNDER_LINE).append(clientType);
		}
		return builder.toString();
	}

}
