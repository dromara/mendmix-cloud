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

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.security.model.BaseUserInfo;
import com.jeesuite.security.model.UserSession;
import com.jeesuite.spring.InstanceFactory;
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
	
	private static volatile SecurityDelegating instance;

	private SecurityDelegating() {
		decisionProvider = InstanceFactory.getInstance(SecurityDecisionProvider.class);
		sessionManager = new SecuritySessionManager(decisionProvider);
		resourceManager = new SecurityResourceManager(decisionProvider);
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
	public static BaseUserInfo doAuthentication(String name,String password){
		BaseUserInfo userInfo = getInstance().decisionProvider.validateUser(name, password);
		
		UserSession session = getCurrentSession(false);
		session.update(userInfo, getInstance().decisionProvider.sessionExpireIn());
		
		if(getInstance().decisionProvider.multiPointEnable()){
			UserSession otherSession = getInstance().sessionManager.getLoginSessionByUserId(userInfo.getId());
			if(otherSession != null && !otherSession.getSessionId().equals(session.getSessionId())){
				getInstance().sessionManager.removeLoginSession(otherSession.getSessionId());
			}
		}
		getInstance().sessionManager.storgeLoginSession(session);
		getInstance().resourceManager.getUserPermissionCodes(userInfo.getId());
		
		return userInfo;
	}
	
	/**
	 * 鉴权
	 * @param userId
	 * @param uri
	 */
	public static void doAuthorization(UserSession session,String uri) throws UnauthorizedException,ForbiddenAccessException{
		boolean isSuperAdmin = getInstance().decisionProvider.superAdminName().equals(session.getUserName());
		if(!isSuperAdmin && !getInstance().resourceManager.isAnonymous(uri)){
			if(session == null || session.isAnonymous()){
				throw new UnauthorizedException();
			}
			String permssionCode = getInstance().resourceManager.getPermssionCode(uri);
			if(StringUtils.isNotBlank(permssionCode) 
					&& !getInstance().resourceManager.getUserPermissionCodes(session.getUserId()).contains(permssionCode)){
				throw new ForbiddenAccessException();
			}
		}
		//
		if(session != null && !session.isAnonymous()){			
			getInstance().decisionProvider.authorizedPostHandle(session);
		}
	}
	
	public static UserSession getCurrentSession(){
		return getCurrentSession(false);
	}
	
	public static UserSession getCurrentSession(boolean first){
		return getInstance().sessionManager.getSessionIfNotCreateAnonymous(RequestContextHolder.getRequest(), RequestContextHolder.getResponse(),first);
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
	
	public static void refreshResources(){
		getInstance().resourceManager.refreshResources();
	}

    public static void doLogout(){
    	getInstance().sessionManager.destroySessionAndCookies(RequestContextHolder.getRequest(), RequestContextHolder.getResponse());
	}
    
    
}
