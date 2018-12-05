package com.jeesuite.security.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.jeesuite.common.util.TokenGenerator;

public class UserSession {

	private BaseUserInfo userInfo;
	private List<String> scopes = new ArrayList<>();
	private String sessionId;
	private Integer expiresIn;
	private Long expiresAt;
	
	
	public UserSession() {}
	
	public static UserSession create(){
		UserSession session = new UserSession();
		session.sessionId = TokenGenerator.generate();
		return session;
	}
	
	public void update(BaseUserInfo userInfo ,Integer expiresIn){
		this.userInfo = userInfo;
		setExpiresIn(expiresIn);
	}

	public List<String> getScopes() {
		return scopes;
	}

	public void setScopes(List<String> scopes) {
		this.scopes = scopes;
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

	public BaseUserInfo getUserInfo() {
		return userInfo;
	}

	public void setUserInfo(BaseUserInfo userInfo) {
		this.userInfo = userInfo;
	}

	public Serializable getUserId(){
		return userInfo == null ? null : userInfo.getId();
	}
	
	public String getUserName(){
		return userInfo == null ? null : userInfo.getUserName();
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}	
	
}
