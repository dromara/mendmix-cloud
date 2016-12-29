package com.jeesuite.rest.filter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.jeesuite.rest.filter.annotation.AuthIgnore;
import com.jeesuite.rest.response.ResponseCode;
import com.jeesuite.rest.response.RestResponse;

@Priority(5)
public class DefaultWebFilter implements ContainerRequestFilter, ContainerResponseFilter {

	@Context
	HttpServletRequest request;
	
	@Context
	ResourceInfo resourceInfo;
	
	List<FilterHandler> filterHandlers = new ArrayList<>();
	
	int handlerCount;
	
	public void registerHandler(FilterHandler hander){
		filterHandlers.add(hander);
		handlerCount = filterHandlers.size();
		
		Collections.sort(filterHandlers, new Comparator<FilterHandler>() {
			@Override
			public int compare(FilterHandler o1, FilterHandler o2) {
				return o1.getPriority() - o2.getPriority();
			}
		});
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		for (int i = 0; i < handlerCount; i++) {
			filterHandlers.get(i).processRequest(requestContext, request, resourceInfo);
		}
		
		Method method = resourceInfo.getResourceMethod();
		//不需要鉴权
		if(true == method.isAnnotationPresent(AuthIgnore.class)){			
			return;
		}
		
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        
		for (int i = handlerCount; i > 0; i--) {
			filterHandlers.get(i).processRequest(requestContext, request, resourceInfo);
		}
		
		MediaType mediaType = responseContext.getMediaType();
		if (mediaType != null && MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
			Object responseData = responseContext.getEntity();
			RestResponse jsonResponse;

			if (responseData instanceof RestResponse) {
				jsonResponse = (RestResponse) responseData;
			} else {
				jsonResponse = new RestResponse(ResponseCode.成功);
				jsonResponse.setData(responseData);
			}
			responseContext.setStatus(ResponseCode.成功.getCode());

			responseContext.setEntity(jsonResponse);

		}
	}
	
	
}
