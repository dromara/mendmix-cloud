package com.jeesuite.security.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.SimpleCryptUtils;
import com.jeesuite.common.util.TokenGenerator;

public class UserSession {

	private static final String CONTACT_CHAR = "#";
	private static String cryptKey = ResourceUtils.getProperty("auth.session.crypt.key", UserSession.class.getName());
	
	private BaseUserInfo userInfo;
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

	public String getUserId(){
		return userInfo == null ? null : userInfo.getId();
	}
	
	public String getUserName(){
		return userInfo == null ? null : userInfo.getUserName();
	}
	
	public String toEncodeString() {

		StringBuilder builder = new StringBuilder();
		builder.append(sessionId);
		if (isAnonymous() == false) {
			builder.append(CONTACT_CHAR);
			builder.append(getUserId()).append(CONTACT_CHAR);
			if (StringUtils.isNotBlank(getUserName())) {
				builder.append(getUserName());
			}
		}
		return SimpleCryptUtils.encrypt(cryptKey, builder.toString());
	}

	
	public static UserSession decode(String encodeString) {
		if (StringUtils.isBlank(encodeString))
			return null;
		encodeString = SimpleCryptUtils.decrypt(cryptKey,encodeString);
		String[] splits = encodeString.split(CONTACT_CHAR); 

		UserSession session = new UserSession();
		session.setSessionId(splits[0]);

		if (splits.length > 1) {
			BaseUserInfo userInfo = new BaseUserInfo();
			userInfo.setId(splits[1]);
			userInfo.setUserName(splits[2]);
			session.setUserInfo(userInfo);
		}

		return session;
	}

	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}	
	
}
