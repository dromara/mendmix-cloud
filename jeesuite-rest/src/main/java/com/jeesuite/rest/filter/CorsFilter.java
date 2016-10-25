package com.jeesuite.rest.filter;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;

import com.jeesuite.rest.RestConst;

/**
 * 跨域
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年3月3日
 * @Copyright (c) 2015, vakinge@github
 */
@Priority(10)
public class CorsFilter implements ContainerResponseFilter {


	@Context
	private ResourceInfo resourceInfo;

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		//允许跨域
		responseContext.getHeaders().add(RestConst.ACCESS_CONTROL_ALLOW_ORIGIN, RestConst.ALLOW_ORIGIN);
		responseContext.getHeaders().add(RestConst.ACCESS_CONTROL_ALLOW_METHODS_TITLE, RestConst.ACCESS_CONTROL_ALLOW_METHODS);
		responseContext.getHeaders().add(RestConst.ACCESS_CONTROL_ALLOW_HEADERS, RestConst.ALLOW_HEADERS);
				
	}

}
