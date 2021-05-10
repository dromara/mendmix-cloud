package com.jeesuite.security.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.TokenGenerator;

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
	private AuthUser userInfo;
	private Integer expiresIn;
	private Long expiresAt;
	private String profile;
	private String tenantId;
	
	
	public UserSession() {}
	
	public static UserSession create(){
		UserSession session = new UserSession();
		session.sessionId = TokenGenerator.generate();
		return session;
	}
	
	public void update(AuthUser userInfo ,Integer expiresIn){
		this.userInfo = userInfo;
		if(userInfo.getTenantScopes() != null && !userInfo.getTenantScopes().isEmpty()) {
			setTenantId(userInfo.getTenantScopes().get(0));
		}
		setExpiresIn(expiresIn);
	}

	public Integer getExpiresIn() {
		return expiresIn;
	}
	public void setExpiresIn(Integer expiresIn) {
		this.expiresIn = expiresIn;
		this.expiresAt = System.currentTimeMillis()/1000 + this.expiresIn;
	}

	public boolean isAnonymous(){
		return userInfo == null;
	}


	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Long getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Long expiresAt) {
		this.expiresAt = expiresAt;
	}

	public AuthUser getUserInfo() {
		return userInfo;
	}

	public void setUserInfo(AuthUser userInfo) {
		this.userInfo = userInfo;
	}


	public String getProfile() {
		return profile;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getUserId(){
		return userInfo == null ? null : userInfo.getId();
	}
	
	public String encodeBaseUser() {
		if(userInfo == null)return  null;
		String info = String.format("%s#%s", userInfo.getId(),userInfo.getUsername());
		return Base64.getEncoder().encodeToString(info.getBytes(StandardCharsets.UTF_8));
	}
	
}
