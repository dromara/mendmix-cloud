package com.jeesuite.springweb.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.model.AuthUser;

public class MockLoginUserInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
    	
    	if(CurrentRuntimeContext.getCurrentUser() == null){
    		AuthUser authUser = new AuthUser();
    		authUser.setId("1");
    		authUser.setName("admin");
    		CurrentRuntimeContext.setAuthUser(authUser);
    	}
    	return true;
    }
}