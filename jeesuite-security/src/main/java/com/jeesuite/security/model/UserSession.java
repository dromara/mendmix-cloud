package com.jeesuite.security.model;

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
	
	
	public UserSession() {}
	
	public static UserSession create(){
		UserSession session = new UserSession();
		session.sessionId = TokenGenerator.generate();
		return session;
	}
	
	public void update(AuthUser userInfo ,Integer expiresIn){
		this.userInfo = userInfo;
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

	public String getUserId(){
		return userInfo == null ? null : userInfo.getId();
	}
	
}
