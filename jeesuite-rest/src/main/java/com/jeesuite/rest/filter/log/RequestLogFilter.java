package com.jeesuite.rest.filter.log;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jeesuite.rest.RequestHeader;
import com.jeesuite.rest.filter.auth.RequestHeaderHolder;
import com.jeesuite.rest.utils.RequestUtils;

@Priority(5)
public class RequestLogFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static final Logger logger = LogManager.getLogger(RequestLogFilter.class);

	public static final String REQUEST_LOG_PROPERTY = "REQUEST_LOG_PROPERTY";
	
	//环境
	private static String env = System.getProperty("disconf.env");
	
	@Context
	HttpServletRequest request;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		
		RequestHeader header = RequestHeaderHolder.get();
		
		if(logger.isDebugEnabled()){
			logger.debug("=====Request Parameters=====\n{}\n",getAndFormatParameters(requestContext,request,header));
		}
		
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		
	}
	
	private String getAndFormatParameters(ContainerRequestContext requestContext,HttpServletRequest request,RequestHeader header){
		StringBuilder sb = new StringBuilder();
		try {			
			Map<String, Object> map = RequestUtils.getParametersMap(requestContext,request);
			if(map != null){				
				Iterator<Map.Entry<String, 	Object>> it = map.entrySet().iterator();  
				while(it.hasNext()){  
					Map.Entry<String, Object> entry=it.next(); 
					sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
				}
			}
		} catch (Exception e) {
		}
		return sb.toString();
	}
}
