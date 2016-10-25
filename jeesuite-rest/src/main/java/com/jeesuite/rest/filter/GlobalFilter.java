package com.jeesuite.rest.filter;

import java.io.IOException;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.rest.RequestHeader;
import com.jeesuite.rest.RestConst;
import com.jeesuite.rest.filter.auth.RequestHeaderHolder;

/**
 * 全局过滤器
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年1月19日
 * @Copyright (c) 2015, vakinge@github
 */
@Priority(1)
public class GlobalFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static Logger logger = LoggerFactory.getLogger(GlobalFilter.class);
	
	//private static MapCacheProvider localCache = new MapCacheProvider();

	@Context
	HttpServletRequest request;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		
		String requestId = request.getParameter(RestConst.REQUEST_ID_PRAMS_NAME);
		

//		if(localCache.exists(requestId)){
//			throw new LifesenseBaseException(400, "请勿重复提交");
//		}
		
		
		RequestHeader header = new RequestHeader();

		//记录requestid防止重复提交
		//localCache.set(requestId, 1, 30);
		//记录开始时间
		if(logger.isDebugEnabled()){
			long currentTime = System.currentTimeMillis();
			requestContext.setProperty(RestConst.PROP_REQUEST_BEGIN_TIME, currentTime);
		}
		
		//放入线程变量
		RequestHeaderHolder.set(header);
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		//localCache.remove(RequestHeaderHolder.get().getRequestId());
		RequestHeaderHolder.clear();
	}
	

}
