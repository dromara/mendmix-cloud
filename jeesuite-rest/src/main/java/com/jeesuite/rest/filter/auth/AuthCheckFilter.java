package com.jeesuite.rest.filter.auth;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;

import com.jeesuite.rest.filter.auth.annotation.AuthIgnore;

/**
 * 权限检查过滤器
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年1月19日
 * @Copyright (c) 2015, vakinge@github
 */
@Priority(2)
public class AuthCheckFilter implements ContainerRequestFilter {

	
	@Context
	HttpServletRequest request;

	@Context
	ResourceInfo resourceInfo;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {

		Method method = resourceInfo.getResourceMethod();
		//不需要鉴权
		if(true == method.isAnnotationPresent(AuthIgnore.class)){			
			return;
		}
		
	}
}
