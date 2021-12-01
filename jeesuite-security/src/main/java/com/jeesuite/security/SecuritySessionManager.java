package com.jeesuite.security;

import java.io.Serializable;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.GlobalRuntimeContext;
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

	public SecuritySessionManager(SecurityDecisionProvider decisionProvider,SecurityStorageManager storageManager) {
		this.storageManager = storageManager;
		this.cookieDomain = decisionProvider.cookieDomain();
		this.sessionIdName = decisionProvider.sessionIdName();
		this.headerTokenName = decisionProvider.headerTokenName();
		this.keepCookie = decisionProvider.keepCookie();
		this.sessionExpireIn = decisionProvider.sessionExpireIn();
		//
		this.storageManager.addCahe(cacheName, this.sessionExpireIn);
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
			if(sessionId != null &&isDevTestEnv) {
				session.setSessionId(sessionId);
			}
			HttpServletRequest request = CurrentRuntimeContext.getRequest();
			Cookie cookie = createSessionCookies(request, session.getSessionId(), sessionExpireIn);
			CurrentRuntimeContext.getResponse().addCookie(cookie);
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

	private String getSessionId() {
		HttpServletRequest request = CurrentRuntimeContext.getRequest();
		return getSessionId(request);
	}

	private String getSessionId(HttpServletRequest request) {
		String sessionId = request.getHeader(headerTokenName);
		if (StringUtils.isNotBlank(sessionId) && sessionId.length() >= 32) {
			return sessionId;
		}

		sessionId = null;
		final Cookie[] oCookies = request.getCookies();
		if (oCookies != null) {
			for (final Cookie item : oCookies) {
				final String name = item.getName();
				if (sessionIdName.equals(name)) {
					return item.getValue();
				}
			}
		}

		return sessionId;
	}

	public String destroySessionAndCookies(HttpServletRequest request, HttpServletResponse response) {

		String sessionId = getSessionId(request);
		if (StringUtils.isNotBlank(sessionId)) {
			removeLoginSession(sessionId);
			//
			response.addCookie(createSessionCookies(request, StringUtils.EMPTY, 0));
		}
		return sessionId;
	}

	private Cookie createSessionCookies(HttpServletRequest request, String sessionId, int expire) {
		String domain = this.cookieDomain;
		if (domain == null) {
			domain = request.getServerName();
			if (request.getServerPort() != 80 && request.getServerPort() != 443) {
				domain = domain + ":" + request.getServerPort();
			}
		}
		Cookie cookie = new Cookie(sessionIdName, sessionId);
		cookie.setDomain(domain);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		if (expire == 0 || !keepCookie) {
			cookie.setMaxAge(expire);
		}
		return cookie;
	}

}
