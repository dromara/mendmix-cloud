package com.jeesuite.security;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.GlobalRuntimeContext;
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

	private final static String SESSION_UID_CACHE_KEY = "uid:%s";
	private static String cacheName = "session";

	private volatile String cookieDomain;
	private String headerTokenName;
	private String sessionIdName;
	private boolean keepCookie;
	private int sessionExpireIn = 0;
	
	private boolean isDevTestEnv = "dev|local|test".contains(GlobalRuntimeContext.ENV);

	private SecurityStorageManager storageManager;
	
	private RequestContextAdapter requestContextAdapter;

	public SecuritySessionManager(SecurityDecisionProvider decisionProvider,SecurityStorageManager storageManager) {
		this.storageManager = storageManager;
		this.cookieDomain = decisionProvider.cookieDomain();
		this.sessionIdName = decisionProvider.sessionIdName();
		this.headerTokenName = decisionProvider.headerTokenName();
		this.keepCookie = decisionProvider.keepCookie();
		this.sessionExpireIn = decisionProvider.sessionExpireIn();
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
		return getSession(true);
	}

	public UserSession getSession(boolean createIfAbsent) {
		String sessionId = getSessionId();
		
		UserSession session = null;
		if (StringUtils.isNotBlank(sessionId)) {
			session = getLoginSession(sessionId);
		} 
		if (createIfAbsent && session == null) {
			session = UserSession.create();
			if(sessionId != null && isDevTestEnv) {
				session.setSessionId(sessionId);
			}
			int expire = keepCookie ? sessionExpireIn : -1;
			requestContextAdapter.addCookie(cookieDomain, cookieDomain, session.getSessionId(), expire);
			//
			storageLoginSession(session);
		}

		return session;

	}

	public UserSession getLoginSessionByUserId(Serializable serializable) {
		String key = String.format(SESSION_UID_CACHE_KEY, serializable);
		String sessionId = storageManager.getCache(cacheName).getString(key);
		if (StringUtils.isBlank(sessionId))
			return null;
		return getLoginSession(sessionId);
	}

	public void storageLoginSession(UserSession session) {
		String key = session.getSessionId();
		storageManager.getCache(cacheName).setObject(key, session);
		if (!session.isAnonymous()) {
			session.setExpiredAt(System.currentTimeMillis() + sessionExpireIn * 1000);
			key = String.format(SESSION_UID_CACHE_KEY, session.getUser().getId());
			storageManager.getCache(cacheName).setString(key, session.getSessionId());
		}
	}
	

	public void removeLoginSession(String sessionId) {
		String key = sessionId;
		UserSession session = getLoginSession(sessionId);
		if (session != null && !session.isAnonymous()) {
			storageManager.getCache(cacheName).remove(key);
			key = String.format(SESSION_UID_CACHE_KEY, session.getUser().getId());
			storageManager.getCache(cacheName).remove(key);
		}
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

}
