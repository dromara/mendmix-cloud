/*
 * Copyright 2016-2018 www.jeesuite.com.
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
package com.jeesuite.security;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.constants.PermissionLevel;
import com.jeesuite.common.exception.ForbiddenAccessException;
import com.jeesuite.common.exception.UnauthorizedException;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.security.model.AccessToken;
import com.jeesuite.security.model.UserSession;
import com.jeesuite.spring.InstanceFactory;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月30日
 */
public class SecurityDelegating {
	
	private SecurityDecisionProvider decisionProvider;
	private SecuritySessionManager sessionManager;
	private SecurityResourceManager resourceManager;
	private SecurityStorageManager storageManager;
	
	private static volatile SecurityDelegating instance;

	private SecurityDelegating() {
		decisionProvider = InstanceFactory.getInstance(SecurityDecisionProvider.class);
		storageManager = new SecurityStorageManager(decisionProvider.cacheType());
		sessionManager = new SecuritySessionManager(decisionProvider,storageManager);
		resourceManager = new SecurityResourceManager(decisionProvider,storageManager);
	}

	private static SecurityDelegating getInstance() {
		if(instance != null)return instance;
		synchronized (SecurityDelegating.class) {
			if(instance != null)return instance;
			instance = new SecurityDelegating();
		}
		return instance;
	}

	protected static SecurityDecisionProvider getConfigurerProvider() {
		return getInstance().decisionProvider;
	}

	
	/**
	 * 认证
	 * @param name
	 * @param password
	 */
	public static UserSession doAuthentication(String type,String name,String password){
		AuthUser userInfo = getInstance().decisionProvider.validateUser(type,name, password);
		return updateSession(userInfo);
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
		session.setUser(userInfo);
		
		if(getInstance().decisionProvider.kickOff()){
			UserSession otherSession = getInstance().sessionManager.getLoginSessionByUserId(userInfo.getId());
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
	public static UserSession doAuthorization() throws UnauthorizedException,ForbiddenAccessException{
		
		UserSession session = getCurrentSession();
		String uri = CurrentRuntimeContext.getRequest().getRequestURI();
		
		boolean isAdmin = session != null && session.getUser() != null 
				&& session.getUser().isAdmin();
		
		if(!isAdmin && !getInstance().resourceManager.isAnonymous(uri)){
			if(session == null || session.isAnonymous()){
				throw new UnauthorizedException();
			}
			
			String permissionKey = ApiPermssionCheckHelper.buildPermissionKey(CurrentRuntimeContext.getRequest().getMethod(), uri);
			PermissionLevel permissionLevel = ApiPermssionCheckHelper.matchPermissionLevel(getInstance().resourceManager, permissionKey);
			
			//如果需鉴权
			if(permissionLevel == PermissionLevel.PermissionRequired){
				List<String> permissions = getInstance().resourceManager.getUserPermissions(session);
				if(!ApiPermssionCheckHelper.checkPermissions(getInstance().resourceManager,permissionKey, permissions)){
					throw new ForbiddenAccessException();
				}
			}
		}
		//
		if(session != null) {
			CurrentRuntimeContext.setAuthUser(session.getUser());
			
			if(StringUtils.isNotBlank(session.getTenantId())) {
				CurrentRuntimeContext.setTenantId(session.getTenantId());
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
	
	public static UserSession genUserSession(String sessionId){
    	return getInstance().sessionManager.getLoginSession(sessionId);
    }
	
	public static boolean validateSessionId(String sessionId){
		UserSession session = getInstance().sessionManager.getLoginSession(sessionId);
		return session != null && session.isAnonymous() == false; 
	}
	
	public static void refreshUserPermssion(Serializable...userIds){
		if(userIds != null && userIds.length > 0 && userIds[1] != null){
			for (Serializable userId : userIds) {
				getInstance().resourceManager.refreshUserPermssions(userId);
			}
		}else{
			getInstance().resourceManager.refreshUserPermssions();
		}
	}

    public static void doLogout(){
    	getInstance().sessionManager.destroySessionAndCookies(CurrentRuntimeContext.getRequest(), CurrentRuntimeContext.getResponse());
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

}
