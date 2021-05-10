package com.jeesuite.security;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.jeesuite.cache.command.RedisObject;
import com.jeesuite.cache.redis.JedisProviderFactory;
import com.jeesuite.common.crypt.Base58;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.security.SecurityConstants.CacheType;
import com.jeesuite.security.cache.LocalCache;
import com.jeesuite.security.cache.RedisCache;
import com.jeesuite.security.model.ExpireableObject;
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
	
	private ThreadLocal<UserSession> createSessionHolder = new ThreadLocal<>();
	private static final String NULL = "null";
	private static final String HEADER_TOKEN_NAME = ResourceUtils.getProperty("security.token.headerName", "x-user-token");
	private final static String SESSION_UID_CACHE_KEY = "uid:%s";

	private Cache cache;
	private volatile String cookieDomain;
	private String sessionIdName = "JSESSIONID";
	private boolean keepCookie;
	private boolean kickOff;
	private int cookieExpireIn = 0;
	//是否共享session
	private boolean sharingSession;
	private com.google.common.cache.Cache<String,Object> localTmpCache;
	
	public SecuritySessionManager(SecurityConfigurerProvider<?> decisionProvider) {
       if(sharingSession = CacheType.redis == decisionProvider.cacheType()){
    	   JedisProviderFactory.addGroupProvider(RedisCache.CACHE_GROUP_NAME);
    	   this.cache = new RedisCache("security.session", decisionProvider.sessionExpireIn());
		}else{
			this.cache = new LocalCache(decisionProvider.sessionExpireIn());
			//
			this.localTmpCache = CacheBuilder
					.newBuilder()
					.maximumSize(5000)
					.expireAfterWrite(15, TimeUnit.MINUTES)
					.build();
		}
		this.cookieDomain = decisionProvider.cookieDomain();
		if(StringUtils.isNotBlank(decisionProvider.sessionIdName())){
			this.sessionIdName = decisionProvider.sessionIdName();
		}
		this.keepCookie = decisionProvider.keepCookie();
		this.kickOff = decisionProvider.kickOff();
		this.cookieExpireIn = decisionProvider.sessionExpireIn();
	}

	public UserSession getLoginSession(String sessionId){
		if(StringUtils.isBlank(sessionId))return null;
		return cache.getObject(sessionId);
	}
	
	
	public UserSession getSessionIfNotCreateAnonymous(HttpServletRequest request,HttpServletResponse response){
		UserSession session = null;
		String sessionId = getSessionId(request);
		if(StringUtils.isNotBlank(sessionId)){
			session = getLoginSession(sessionId);
		}
		if(session == null){
			//避免一次请求调用多次
			session = createSessionHolder.get();
			if(session == null) {
				session = UserSession.create();
				if(response != null){				
					Cookie cookie = createSessionCookies(request,session.getSessionId(), cookieExpireIn);
					response.addCookie(cookie);
				}
				//
				storageLoginSession(session);
				createSessionHolder.set(session);
			}
		}
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
		if(!session.isAnonymous() && !kickOff){			
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
			domain = request.getServerName();
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
		String sessionId = request.getHeader(HEADER_TOKEN_NAME);
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
		String profile = request.getParameter(SecurityConstants.HEADER_AUTH_PROFILE);
		if(isBlank(profile)){
			profile = request.getHeader(SecurityConstants.HEADER_AUTH_PROFILE);
		}
		if(isBlank(profile)){
			Cookie[] cookies = request.getCookies();
			if(cookies == null)return null;
			for (Cookie cookie : cookies) {
				if(SecurityConstants.HEADER_AUTH_PROFILE.equals(cookie.getName())){
					profile = cookie.getValue();
					break;
				}
			}
		}
		return profile;
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
		cookie.setMaxAge(cookieExpireIn);
		
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
	
	public String setTemporaryObject(String name,Object object,int expireInSeconds) {
		String sessionId = getSessionId(CurrentRuntimeContext.getRequest());
		String cacheKey = sessionId == null ? name : String.format("%s:%s", sessionId,name);
		if(sharingSession) {
			new RedisObject(cacheKey,RedisCache.CACHE_GROUP_NAME).set(object, expireInSeconds);
		}else {
			ExpireableObject expireableObject = new ExpireableObject(object, System.currentTimeMillis() + expireInSeconds * 1000);
			localTmpCache.put(cacheKey, expireableObject);
		}
		return Base58.encode(cacheKey.getBytes());
	}
	
	public <T> T getTemporaryObject(String name) {
		String sessionId = getSessionId(CurrentRuntimeContext.getRequest());
		String cacheKey = String.format("%s:%s", sessionId,name);
		return getTemporaryObjectByKey(cacheKey);
	}
	
	public <T> T getTemporaryObjectByEncodeKey(String cacheKey) {
		cacheKey = new String(Base58.decode(cacheKey));
		return  getTemporaryObjectByKey(cacheKey);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getTemporaryObjectByKey(String cacheKey) {
		Object obj = null;
		if(sharingSession) {
			obj = new RedisObject(cacheKey,RedisCache.CACHE_GROUP_NAME).get();
		}else {
			ExpireableObject expireableObject = (ExpireableObject) localTmpCache.getIfPresent(cacheKey);
			if(expireableObject != null && expireableObject.getExpireAt() >= System.currentTimeMillis()) {
				obj = expireableObject.getTarget();
			}
		}
		return (T) obj;
	}

}
