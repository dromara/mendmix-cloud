/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.security.model;

import com.mendmix.common.model.AuthUser;
import com.mendmix.common.util.TokenGenerator;

/**
 * 
 * 
 * <br>
 * Class Name   : UserSession
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年10月25日
 */
public class UserSession {

	private String sessionId;
	private AuthUser user;
	private long expiredAt;
	
	private String tenanId;
	private String systemId;
	private String clientType;

	public UserSession() {}
	
	public UserSession(String sessionId,AuthUser user) {
		this.sessionId = sessionId;
		this.user = user;
	}
	
	public static UserSession create(){
		UserSession session = new UserSession();
		session.sessionId = TokenGenerator.generate();
		return session;
	}
	
	/**
	 * @return the user
	 */
	public AuthUser getUser() {
		return user;
	}

	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}
	/**
	 * @param sessionId the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public void setUser(AuthUser user) {
		this.user = user;
	}
	
	public long getExpiredAt() {
		return expiredAt;
	}
	public void setExpiredAt(long expiredAt) {
		this.expiredAt = expiredAt;
	}
	
	public boolean isAnonymous(){
		return user == null;
	}
	
	public int getExpiresIn() {
		return (int) (this.expiredAt - System.currentTimeMillis());
	}

	public String getTenanId() {
		return tenanId;
	}

	public void setTenanId(String tenanId) {
		this.tenanId = tenanId;
	}

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public String getClientType() {
		return clientType;
	}

	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

}
