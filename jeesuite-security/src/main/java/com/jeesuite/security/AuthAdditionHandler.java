package com.jeesuite.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeesuite.security.model.UserSession;

public interface AuthAdditionHandler {

	void beforeAuthentication(HttpServletRequest request,HttpServletResponse response);
	
	boolean customAuthentication(HttpServletRequest request);
	
	void afterAuthentication(UserSession userSession);
}
