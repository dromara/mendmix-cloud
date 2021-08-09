package com.jeesuite.zuul.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jeesuite.common.model.AuthUser;

/**
 * 
 * <br>
 * Class Name   : UserSession
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年3月9日
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
	
	
}
