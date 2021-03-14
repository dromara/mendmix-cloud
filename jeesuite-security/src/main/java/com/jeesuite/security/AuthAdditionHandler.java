package com.jeesuite.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface AuthAdditionHandler {

	void beforeAuthorization(HttpServletRequest request,HttpServletResponse response);
	
	void afterAuthorization(HttpServletRequest request,HttpServletResponse response);
}
