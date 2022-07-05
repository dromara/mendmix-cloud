/*
 * Copyright 2016-2018 www.mendmix.com.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.constants.PermissionLevel;
import com.mendmix.common.exception.ForbiddenAccessException;
import com.mendmix.common.exception.UnauthorizedException;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.util.TokenGenerator;
import com.mendmix.security.model.AccessToken;
import com.mendmix.security.model.ApiPermission;
import com.mendmix.security.model.UserSession;
import com.mendmix.security.util.ApiPermssionHelper;
import com.mendmix.spring.InstanceFactory;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月30日
 */
public class SecurityDelegating {
	
	private static Logger logger = LoggerFactory.getLogger("com.mendmix.security");
	
	private static final int SESSION_INTERVAL_MILLS =  60 * 1000;
	private SecurityDecisionProvider decisionProvider;
	private SecuritySessionManager sessionManager;
	private SecurityStorageManager storageManager;
	
	private static volatile SecurityDelegating instance;

	private SecurityDelegating() {
		decisionProvider = InstanceFactory.getInstance(SecurityDecisionProvider.class);
		storageManager = new SecurityStorageManager(decisionProvider.cacheType());
		sessionManager = new SecuritySessionManager(decisionProvider,storageManager);
		logger.info("MENDMIX-TRACE-LOGGGING-->> SecurityDelegating inited !!,sessisonStorageType:{}",decisionProvider.cacheType());
	}

	private static SecurityDelegating getInstance() {
		if(instance != null)return instance;
		synchronized (SecurityDelegating.class) {
			if(instance != null)return instance;
			instance = new SecurityDelegating();
		}
		return instance;
	}
	
	public static void init() {
		getInstance();
	}

	public static SecurityDecisionProvider decisionProvider() {
		return getInstance().decisionProvider;
	}

	
	/**
	 * 认证
	 * @param name
	 * @param password
	 */
	public static UserSession doAuthentication(String type,String name,String password){
		AuthUser userInfo = getInstance().decisionProvider.validateUser(type,name, password);
		UserSession session = updateSession(userInfo);
		if(getInstance().decisionProvider.apiAuthzEnabled()) {
			getInstance().fetchUserPermissions(session);
		}
		return session;
	}
	
	public static String doAuthenticationForOauth2(String type,String name,String password){
		AuthUser userInfo = getInstance().decisionProvider.validateUser(type,name, password);
		String authCode = TokenGenerator.generate();
		setTemporaryCacheValue(authCode, userInfo, 60);
		return authCode;
	}
	
	public static String oauth2AuthCode2UserId(String authCode){
		AuthUser userInfo = getTemporaryCacheValue(authCode);
		return userInfo == null ? null : userInfo.getId();
	}
	
	public static AccessToken createOauth2AccessToken(AuthUser user){
		UserSession session = getCurrentSession();
		session.setUser(user);
		getInstance().sessionManager.storageLoginSession(session);
		//
		AccessToken accessToken = new AccessToken();
		accessToken.setAccess_token(session.getSessionId());
		accessToken.setRefresh_token(TokenGenerator.generate());
		accessToken.setExpires_in(session.getExpiresIn());
		return accessToken;
	}
	
	public static UserSession updateSession(AuthUser userInfo){
		UserSession session = getCurrentSession();
		if(session == null)session = UserSession.create();
		//多系统情况，已第一次登录的系统id为准
		if(session.getSystemId() == null) {
			session.setSystemId(CurrentRuntimeContext.getSystemId());
		}
		session.setTenanId(CurrentRuntimeContext.getTenantId());
		session.setClientType(CurrentRuntimeContext.getClientType());
		session.setUser(userInfo);
		
		if(getInstance().decisionProvider.kickOff()){
			UserSession otherSession = getInstance().sessionManager.getLoginSessionByUserId(userInfo);
			if(otherSession != null && !otherSession.getSessionId().equals(session.getSessionId())){
				getInstance().sessionManager.removeLoginSession(otherSession.getSessionId());
			}
		}
		getInstance().sessionManager.storageLoginSession(session);
		
		return session;
	}
	
	/**
	 * 鉴权
	 * @param userId
	 * @param uri
	 */
	public static UserSession doAuthorization(String method,String uri) throws UnauthorizedException,ForbiddenAccessException{
		
		UserSession session = getCurrentSession();
		//续租
		if(session != null) {
			long interval = System.currentTimeMillis() - getInstance().sessionManager.getUpdateTime(session);
			if(interval > SESSION_INTERVAL_MILLS) {
				getInstance().sessionManager.storageLoginSession(session);
			}
		}

		boolean isAdmin = session != null && session.getUser() != null && session.getUser().isAdmin();
		
		
		ApiPermission permissionObject = ApiPermssionHelper.matchPermissionObject(method, uri);
		if((session == null || session.isAnonymous()) && PermissionLevel.Anonymous != permissionObject.getPermissionLevel()) {
			throw new UnauthorizedException();
		}
		//兼容多系统切换
		if(session != null) {
			session.setSystemId(CurrentRuntimeContext.getSystemId());
		}
		
		if(!isAdmin && getInstance().decisionProvider.apiAuthzEnabled()) {
			//如果需鉴权
			if(permissionObject.getPermissionLevel() == PermissionLevel.PermissionRequired){
				List<String> permissions = getInstance().getUserPermissions(session);
				if(!permissions.contains(permissionObject.getPermissionKey())){
					throw new ForbiddenAccessException();
				}
			}
		}

		return session;
	}
	
	
	public static UserSession getAndValidateCurrentSession(){
		UserSession session = getCurrentSession();
		if(session == null || session.isAnonymous()){
			throw new UnauthorizedException();
		}
		return session;
	}
	
	public static UserSession getCurrentSession(){
		UserSession session = getInstance().sessionManager.getSession();
		return session;
	}
	
	public static String getCurrentSessionId(){
		return getInstance().sessionManager.getSessionId();
	}
	
	public static UserSession genUserSession(String sessionId){
    	return getInstance().sessionManager.getLoginSession(sessionId);
    }
	
	public static boolean validateSessionId(String sessionId){
		UserSession session = getInstance().sessionManager.getLoginSession(sessionId);
		return session != null && session.isAnonymous() == false; 
	}
	
	public static void refreshUserPermssion(Serializable...userIds){
		if(userIds != null && userIds.length > 0 && userIds[1] != null){
			
		}else{
			
		}
	}

    public static void doLogout(){
    	getInstance().sessionManager.destroySessionAndCookies();
	}
    
    public static void setSessionAttribute(String name, Object object) {
    	getInstance().sessionManager.setSessionAttribute(name, object);
    }

	public static <T> T getSessionAttribute(String name) {
		return getInstance().sessionManager.getSessionAttribute(name);
	}
    
    public static void setTemporaryCacheValue(String key, Object value, int expireInSeconds) {
    	getInstance().storageManager.setTemporaryCacheValue(key, value, expireInSeconds);
    }
    
    public static <T> T getTemporaryCacheValue(String key) {
    	return getInstance().storageManager.getTemporaryCacheValue(key);
    }
    
    private List<String> getUserPermissions(UserSession session){
    	List<String> permissions = sessionManager.getUserPermissions(session);
    	if(permissions != null)return permissions;
    	synchronized (getInstance()) {
    		permissions = fetchUserPermissions(session);
    		return permissions;
		}
    }

	private List<String> fetchUserPermissions(UserSession session) {
		List<String> permissions;
		List<ApiPermission> apiPermissions = decisionProvider.getUserApiPermissions(session.getUser().getId());
		if(apiPermissions == null || apiPermissions.isEmpty()) {
			permissions = new ArrayList<>(0);
		}else {
			permissions = new ArrayList<>(apiPermissions.size());
			for (ApiPermission api : apiPermissions) {
				permissions.add(ApiPermssionHelper.buildPermissionKey(api.getMethod(), api.getUri()));
			}
		}
		sessionManager.updateUserPermissions(session, permissions);
		return permissions;
	}

}
