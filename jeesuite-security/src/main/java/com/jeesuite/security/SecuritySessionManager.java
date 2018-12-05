package com.jeesuite.security;

import java.io.Serializable;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.security.model.UserSession;
import com.jeesuite.springweb.utils.WebUtils;

/**
 * session管理器
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年12月4日
 */
public class SecuritySessionManager {
	
	private static final String NULL = "null";
	private static final String ACCESSTOKEN = "accessToken";
	
	//auth cache keys
	private static final String SESSION_CACHE_KEY = "auth:session:%s";
	private final static String LOGIN_UID_CACHE_KEY = "auth:uid:%s";

	private Cache cache;
	private volatile String cookieDomain;
	private String sessionIdName = "JSESSION_ID";
	private boolean keepCookie;
	private boolean multiPointEnable;
	
	public SecuritySessionManager(SecurityDecisionProvider decisionProvider, Cache cache) {
		this.cache = cache;
		this.cookieDomain = decisionProvider.cookieDomain();
		if(StringUtils.isNotBlank(decisionProvider.sessionIdName())){
			this.sessionIdName = decisionProvider.sessionIdName();
		}
		this.keepCookie = decisionProvider.keepCookie();
		this.multiPointEnable = decisionProvider.multiPointEnable();
	}

	public UserSession getLoginSession(String sessionId){
		if(StringUtils.isBlank(sessionId))return null;
		String key = String.format(SESSION_CACHE_KEY, sessionId);
		return cache.getObject(key);
	}
	
	
	public UserSession getSessionIfNotCreateAnonymous(HttpServletRequest request,HttpServletResponse response){
		UserSession session = null;
		String sessionId = getSessionId(request);
		if(StringUtils.isNotBlank(sessionId)){
			session = getLoginSession(sessionId);
		}
		
		if(session == null){			
			session = UserSession.create();
			//storgeLoginSession(session);
			Cookie cookie = createSessionCookies(request,session.getSessionId(), cookieDomain, session.getExpiresIn());
			response.addCookie(cookie);
		}

		return session;
	}
	
	public UserSession getLoginSessionByUserId(Serializable  serializable){
		String key = String.format(LOGIN_UID_CACHE_KEY, serializable);
		String sessionId = cache.getString(key);
		if(StringUtils.isBlank(sessionId))return null;
		return  getLoginSession(sessionId);
	}
	
	public void storgeLoginSession(UserSession session){
		String key = String.format(SESSION_CACHE_KEY, session.getSessionId());
		cache.setObject(key,session);
		if(!session.isAnonymous() && multiPointEnable){			
			key = String.format(LOGIN_UID_CACHE_KEY, session.getUserId());
			cache.setString(key, session.getSessionId());
		}
	}
	
	public void removeLoginSession(String sessionId){
		String key = String.format(SESSION_CACHE_KEY, sessionId);
		UserSession session = getLoginSession(sessionId);
		if(session != null){
			cache.remove(key);
			key = String.format(LOGIN_UID_CACHE_KEY, session.getUserId());
			cache.remove(key);
		}
	}

    
	private Cookie createSessionCookies(HttpServletRequest request,String sessionId,String domain,int expire){
		if(cookieDomain == null){
			cookieDomain = WebUtils.getRootDomain(request);
		}
		Cookie cookie = new Cookie(sessionIdName,sessionId);  
		cookie.setDomain(domain);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		if(expire == 0 || !keepCookie){			
			cookie.setMaxAge(expire);
		}
		return cookie;
	}
	
	
	/**
	 * 获取授权Id （accessToken or  sessionId）
	 * @param request
	 * @return
	 */
	public String getSessionId(HttpServletRequest request) {
		String sessionId = request.getParameter(ACCESSTOKEN);
		if(isBlank(sessionId)){
			sessionId = request.getHeader(ACCESSTOKEN);
		}
		if(isBlank(sessionId)){
			Cookie[] cookies = request.getCookies();
			if(cookies == null)return null;
			for (Cookie cookie : cookies) {
				if(sessionIdName.equals(cookie.getName())){
					sessionId = cookie.getValue();
					break;
				}
			}
		}
		return sessionId;
	}
	
	private static boolean isBlank(String str){
		return StringUtils.isBlank(str) || NULL.equals(str);
	}
	
	public String destroySessionAndCookies(HttpServletRequest request,HttpServletResponse response) {
		
		String sessionId = getSessionId(request);
		if(StringUtils.isNotBlank(sessionId)){
			removeLoginSession(sessionId);
			//
			response.addCookie(createSessionCookies(request,StringUtils.EMPTY, cookieDomain, 0));
		}
		return sessionId;
	}

}
