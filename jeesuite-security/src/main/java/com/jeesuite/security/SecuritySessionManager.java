package com.jeesuite.security;

import java.io.Serializable;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseCookie.ResponseCookieBuilder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.ThreadLocalContext;
import com.jeesuite.security.model.UserSession;

/**
 * session管理器
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年12月4日
 */
public class SecuritySessionManager {

	private static final String CTX_REQUEST_NAME = "ctx_request_obj";
	private final static String SESSION_UID_CACHE_KEY = "uid:%s";
	private static String cacheName = "session";

	private static String cookieDomain;
	private static String headerTokenName;
	private static String sessionIdName;
	private static boolean keepCookie;
	private static int sessionExpireIn = 0;
	
	private boolean isDevTestEnv = "dev|local|test".contains(GlobalRuntimeContext.ENV);

	private SecurityStorageManager storageManager;
	
	public static void init(HttpServletRequest request,HttpServletResponse response) {
		if (cookieDomain == null) {
			cookieDomain = request.getServerName();
			if (request.getServerPort() != 80 && request.getServerPort() != 443) {
				cookieDomain = cookieDomain + ":" + request.getServerPort();
			}
		}
		ThreadLocalContext.set(CTX_REQUEST_NAME, new RequestResponsePair(request, response, true));
	}
	
	public static void init(ServerHttpRequest request,ServerHttpResponse response) {
		if (cookieDomain == null) {
			cookieDomain = request.getRemoteAddress().getHostName();
			if (request.getLocalAddress().getPort() != 80 && request.getLocalAddress().getPort() != 443) {
				cookieDomain = cookieDomain + ":" + request.getLocalAddress().getPort();
			}
		}
		ThreadLocalContext.set(CTX_REQUEST_NAME, new RequestResponsePair(request, response, false));
	}
	
	private RequestResponsePair getRequestResponsePair() {
		return ThreadLocalContext.get(CTX_REQUEST_NAME);
	}

	public SecuritySessionManager(SecurityDecisionProvider decisionProvider,SecurityStorageManager storageManager) {
		this.storageManager = storageManager;
		cookieDomain = decisionProvider.cookieDomain();
		sessionIdName = decisionProvider.sessionIdName();
		headerTokenName = decisionProvider.headerTokenName();
		keepCookie = decisionProvider.keepCookie();
		sessionExpireIn = decisionProvider.sessionExpireIn();
		//
		this.storageManager.addCahe(cacheName, sessionExpireIn);
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
			getRequestResponsePair().addSessionCookies(session.getSessionId(), sessionExpireIn);
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
		return getRequestResponsePair().getSessionId();
	}

	

	public String destroySessionAndCookies() {

		String sessionId = getRequestResponsePair().getSessionId();
		if (StringUtils.isNotBlank(sessionId)) {
			removeLoginSession(sessionId);
			//
			getRequestResponsePair().addSessionCookies(StringUtils.EMPTY, 0);
		}
		return sessionId;
	}

	
	private static class RequestResponsePair {
		Object request;
		Object response;
		boolean servlet;
		
		public RequestResponsePair(Object request, Object response, boolean servlet) {
			super();
			this.request = request;
			this.response = response;
			this.servlet = servlet;
		}
		
		public void addSessionCookies(String sessionId,int expire) {
			if(servlet) {
				Cookie cookie = new Cookie(sessionIdName, sessionId);
				cookie.setDomain(cookieDomain);
				cookie.setPath("/");
				cookie.setHttpOnly(true);
				if (expire == 0 || !keepCookie) {
					cookie.setMaxAge(expire);
				}
			}else {
				ResponseCookieBuilder cookieBuilder = ResponseCookie.from(sessionIdName, sessionId).domain(cookieDomain).httpOnly(true);
				if (expire == 0 || !keepCookie) {
					cookieBuilder.maxAge(expire);
				}
				((ServerHttpResponse)response).addCookie(cookieBuilder.build());
			}
		}
		
		
		public String getSessionId() {
			String sessionId = null;
			if(servlet) {
				HttpServletRequest servletRequest = (HttpServletRequest) request;
				sessionId = servletRequest.getHeader(headerTokenName);
				if (StringUtils.isNotBlank(sessionId) && sessionId.length() >= 32) {
					return sessionId;
				}
				final Cookie[] oCookies = servletRequest.getCookies();
				if (oCookies != null) {
					String name;
					for (final Cookie item : oCookies) {
						name = item.getName();
						if (sessionIdName.equals(name)) {
							return item.getValue();
						}
					}
				}
			}else {
				ServerHttpRequest httpRequest = (ServerHttpRequest) request;
				sessionId = httpRequest.getHeaders().getFirst(headerTokenName);
				if (StringUtils.isNotBlank(sessionId) && sessionId.length() >= 32) {
					return sessionId;
				}
				MultiValueMap<String, HttpCookie> cookies = httpRequest.getCookies();
				if(cookies != null && cookies.containsKey(sessionIdName)) {
					return cookies.get(sessionIdName).get(0).getValue();
				}
			}

			return sessionId;
		}

	}

}
