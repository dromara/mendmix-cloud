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

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.model.AuthUser;
import com.jeesuite.security.model.AccessToken;
import com.jeesuite.security.model.UserSession;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.springweb.CurrentRuntimeContext;
import com.jeesuite.springweb.exception.ForbiddenAccessException;
import com.jeesuite.springweb.exception.UnauthorizedException;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月30日
 */
public class SecurityDelegating {
	
	private SecurityDecisionProvider decisionProvider;
	private SecuritySessionManager sessionManager;
	private SecurityResourceManager resourceManager;
	private SecurityTicketManager ticketManager;
	private SecurityOauth2Manager oauth2Manager;
	
	private static volatile SecurityDelegating instance;

	private SecurityDelegating() {
		decisionProvider = InstanceFactory.getInstance(SecurityDecisionProvider.class);
		sessionManager = new SecuritySessionManager(decisionProvider);
		ticketManager = new SecurityTicketManager(decisionProvider);
		resourceManager = new SecurityResourceManager(decisionProvider);
		if(decisionProvider.oauth2Enabled()){
			oauth2Manager = new SecurityOauth2Manager(decisionProvider);
		}
	}

	public static SecurityDelegating getInstance() {
		if(instance != null)return instance;
		synchronized (SecurityDelegating.class) {
			if(instance != null)return instance;
			instance = new SecurityDelegating();
		}
		return instance;
	}

	/**
	 * 认证
	 * @param name
	 * @param password
	 */
	public static UserSession doAuthentication(String name,String password){
		AuthUser userInfo = getInstance().decisionProvider.validateUser(name, password);
		
		UserSession session = getCurrentSession(false);

		session.update(userInfo, getInstance().decisionProvider.sessionExpireIn());
		
		if(getInstance().decisionProvider.ssoEnabled()){
			UserSession otherSession = getInstance().sessionManager.getLoginSessionByUserId(userInfo.getId());
			if(otherSession != null && !otherSession.getSessionId().equals(session.getSessionId())){
				getInstance().sessionManager.removeLoginSession(otherSession.getSessionId());
			}
		}
		getInstance().sessionManager.storageLoginSession(session);
		
		return session;
	}
	
	public static String doAuthenticationForOauth2(String name,String password){
		AuthUser userInfo = getInstance().decisionProvider.validateUser(name, password);
		return getInstance().oauth2Manager.createOauth2AuthCode(userInfo.getId());
	}
	
	public static String oauth2AuthCode2UserId(String authCode){
		return getInstance().oauth2Manager.authCode2UserId(authCode);
	}
	
	public static AccessToken createOauth2AccessToken(AuthUser user){
		return getInstance().oauth2Manager.createAccessToken(user);
	}
	
	public static UserSession updateSession(AuthUser userInfo){
		UserSession session = getCurrentSession();
		session.update(userInfo, getInstance().decisionProvider.sessionExpireIn());
		
		if(getInstance().decisionProvider.ssoEnabled()){
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
		
		boolean isSuperAdmin = session != null && session.getUserInfo() != null 
				&& getInstance().decisionProvider.superAdminName().equals(session.getUserInfo().getName());
		if(!isSuperAdmin && !getInstance().resourceManager.isAnonymous(uri)){
			if(session == null || session.isAnonymous()){
				throw new UnauthorizedException();
			}
			String permssionCode = getInstance().resourceManager.getPermssionCode(uri);
			if(StringUtils.isNotBlank(permssionCode) 
					&& !getInstance().resourceManager.getUserPermissionCodes(session).contains(permssionCode)){
				throw new ForbiddenAccessException();
			}
		}
		//
		if(session != null && !session.isAnonymous()){			
			getInstance().decisionProvider.authorizedPostHandle(session);
		}
		//
		CurrentRuntimeContext.setAuthUser(session.getUserInfo());
		
		if(StringUtils.isNotBlank(session.getTenantId())) {
			CurrentRuntimeContext.setTenantId(session.getTenantId());
		}
		
		return session;
	}
	
	public static UserSession getCurrentSession(){
		return getCurrentSession(true);
	}
	
	public static UserSession getAndValidateCurrentSession(){
		UserSession session = getCurrentSession(true);
		if(session == null || session.isAnonymous()){
			throw new UnauthorizedException();
		}
		return session;
	}
	
	private static UserSession getCurrentSession(boolean first){
		HttpServletResponse response = CurrentRuntimeContext.getResponse();
		UserSession session = getInstance().sessionManager.getSessionIfNotCreateAnonymous(CurrentRuntimeContext.getRequest(), response,first);
		//profile
		String profile = getInstance().sessionManager.getCurrentProfile(CurrentRuntimeContext.getRequest());
		if(StringUtils.isBlank(profile)){
			profile = getInstance().decisionProvider.getCurrentProfile(CurrentRuntimeContext.getRequest());
			getInstance().sessionManager.setCurrentProfile(profile);
		}
		session.setProfile(profile);
		
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
	
	public static SecurityDecisionProvider getSecurityDecision(){
		return getInstance().decisionProvider;
	}
	
	public static void refreshResources(){
		getInstance().resourceManager.refreshResources();
	}

    public static void doLogout(){
    	getInstance().sessionManager.destroySessionAndCookies(CurrentRuntimeContext.getRequest(), CurrentRuntimeContext.getResponse());
	}
    
    
    public static String objectToTicket(Object value){
    	return getInstance().ticketManager.setTicketObject(value);
    }
    
    public static <T> T ticketToObject(String ticket){
    	return getInstance().ticketManager.getTicketObject(ticket);
    }
}
