/**
 * 
 */
package com.jeesuite.rest.filter.handler;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;

import com.jeesuite.rest.filter.FilterHandler;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月29日
 */
public class AuthCheckFilter implements FilterHandler{


	@Override
	public void processRequest(ContainerRequestContext requestContext, HttpServletRequest request,ResourceInfo resourceInfo) {
		
	}

	@Override
	public void processResponse(ContainerRequestContext requestContext, ContainerResponseContext responseContext,ResourceInfo resourceInfo) {
		
	}

	@Override
	public int getPriority() {
		return 0;
	}

	
}
