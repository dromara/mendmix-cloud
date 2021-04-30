package com.jeesuite.security;

import java.io.Serializable;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.cache.redis.JedisProviderFactory;
import com.jeesuite.security.SecurityConstants.CacheType;
import com.jeesuite.security.cache.LocalCache;
import com.jeesuite.security.cache.RedisCache;
import com.jeesuite.security.model.UserSession;
import com.jeesuite.security.util.SecurityCryptUtils;
import com.jeesuite.springweb.CurrentRuntimeContext;
import com.jeesuite.springweb.utils.WebUtils;

/**
 * session管理器
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年12月4日
 */
public class SecuritySessionManager {
	
	private ThreadLocal<UserSession> sessionThreadHistory = new ThreadLocal<>();
	private static final String NULL = "null";
	private static final String ACCESSTOKEN = "accessToken";
	private final static String SESSION_UID_CACHE_KEY = "uid:%s";

	private Cache cache;
	private volatile String cookieDomain;
	private String sessionIdName = "JSESSIONID";
	private boolean keepCookie;
	private boolean ssoEnabled;
	private int cookieExpireIn = 0;
	
	public SecuritySessionManager(SecurityConfigurerProvider<?> decisionProvider) {
       if(CacheType.redis == decisionProvider.cacheType()){
    	   JedisProviderFactory.addGroupProvider(RedisCache.CACHE_GROUP_NAME);
    	   this.cache = new RedisCache("security.session", decisionProvider.sessionExpireIn());
		}else{
			this.cache = new LocalCache(decisionProvider.sessionExpireIn());
		}
		this.cookieDomain = decisionProvider.cookieDomain();
		if(StringUtils.isNotBlank(decisionProvider.sessionIdName())){
			this.sessionIdName = decisionProvider.sessionIdName();
		}
		this.keepCookie = decisionProvider.keepCookie();
		this.ssoEnabled = decisionProvider.ssoEnabled();
		this.cookieExpireIn = decisionProvider.sessionExpireIn();
	}

	public UserSession getLoginSession(String sessionId){
		if(StringUtils.isBlank(sessionId))return null;
		return cache.getObject(sessionId);
	}
	
	
	public UserSession getSessionIfNotCreateAnonymous(HttpServletRequest request,HttpServletResponse response,boolean first){
		UserSession session = first ? null : sessionThreadHistory.get();
		if(session == null){			
			String sessionId = getSessionId(request);
			if(StringUtils.isNotBlank(sessionId)){
				session = getLoginSession(sessionId);
			}
		}
		if(session == null){			
			session = UserSession.create();
			if(response != null){				
				Cookie cookie = createSessionCookies(request,session.getSessionId(), cookieExpireIn);
				response.addCookie(cookie);
			}
		}
		if(session != null)sessionThreadHistory.set(session);

		return session;
	}
	
	public UserSession getLoginSessionByUserId(Serializable  serializable){
		String key = String.format(SESSION_UID_CACHE_KEY, serializable);
		String sessionId = cache.getString(key);
		if(StringUtils.isBlank(sessionId))return null;
		return  getLoginSession(sessionId);
	}
	
	public void storageLoginSession(UserSession session){
		String key = session.getSessionId();
		cache.setObject(key,session);
		if(!session.isAnonymous() && !ssoEnabled){			
			key = String.format(SESSION_UID_CACHE_KEY, session.getUserId());
			cache.setString(key, session.getSessionId());
		}
	}

	
	public void removeLoginSession(String sessionId){
		String key = sessionId;
		UserSession session = getLoginSession(sessionId);
		if(session != null){
			cache.remove(key);
			key = String.format(SESSION_UID_CACHE_KEY, session.getUserId());
			cache.remove(key);
		}
	}

    
	private Cookie createSessionCookies(HttpServletRequest request,String sessionId,int expire){
		String domain = this.cookieDomain;
		if(domain == null){
			domain = WebUtils.getRootDomain(request);
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
	
	public String getCurrentProfile(HttpServletRequest request) {
		String sessionId = request.getParameter(SecurityConstants.HEADER_AUTH_PROFILE);
		if(isBlank(sessionId)){
			sessionId = request.getHeader(SecurityConstants.HEADER_AUTH_PROFILE);
		}
		if(isBlank(sessionId)){
			Cookie[] cookies = request.getCookies();
			if(cookies == null)return null;
			for (Cookie cookie : cookies) {
				if(SecurityConstants.HEADER_AUTH_PROFILE.equals(cookie.getName())){
					sessionId = cookie.getValue();
					break;
				}
			}
		}
		
		if(StringUtils.isNotBlank(sessionId)){
			sessionId = SecurityCryptUtils.decrypt(sessionId);
		}
		return sessionId;
	}
	
	public void setCurrentProfile(String profile){
		if(StringUtils.isBlank(profile))return;
		String domain = this.cookieDomain;
		if(domain == null){
			domain = WebUtils.getRootDomain(CurrentRuntimeContext.getRequest());
		}
		profile = SecurityCryptUtils.encrypt(profile);
		Cookie cookie = new Cookie(SecurityConstants.HEADER_AUTH_PROFILE,profile);  
		cookie.setDomain(domain);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		cookie.setMaxAge(3600);
		
		CurrentRuntimeContext.getResponse().addCookie(cookie);
	}
	
	private static boolean isBlank(String str){
		return StringUtils.isBlank(str) || NULL.equals(str);
	}
	
	public String destroySessionAndCookies(HttpServletRequest request,HttpServletResponse response) {
		
		String sessionId = getSessionId(request);
		if(StringUtils.isNotBlank(sessionId)){
			removeLoginSession(sessionId);
			//
			response.addCookie(createSessionCookies(request,StringUtils.EMPTY, 0));
		}
		return sessionId;
	}

}
