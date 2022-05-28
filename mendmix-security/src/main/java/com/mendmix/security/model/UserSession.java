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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

	private String tenantId;
	@JsonIgnore
	private List<String> permissions;

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


	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public void setUser(AuthUser user) {
		this.user = user;
	}

	/**
	 * @return the permissions
	 */
	public List<String> getPermissions() {
		return permissions;
	}
	/**
	 * @param permissions the permissions to set
	 */
	public void setPermissions(List<String> permissions) {
		this.permissions = permissions;
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
	
}
