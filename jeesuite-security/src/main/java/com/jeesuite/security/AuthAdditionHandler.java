package com.jeesuite.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeesuite.security.model.UserSession;

public interface AuthAdditionHandler {

	void beforeAuthorization(HttpServletRequest request,HttpServletResponse response);
	
	void afterAuthorization(UserSession userSession);
}
