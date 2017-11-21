package com.jeesuite.springweb;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestContextHelper {

	private static final ThreadLocal<RequestResponsePair> holder = new ThreadLocal<>();
	
	
	public static void set(HttpServletRequest request, HttpServletResponse response){
		holder.set(new RequestResponsePair(request, response));
	}
	
	public static HttpServletRequest getRequest() {
		return holder.get() == null ? ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest() : holder.get().request;
	}
	
	public static HttpServletResponse getResponse() {
		return holder.get() == null ? null : holder.get().response;
	}
	
	private static class RequestResponsePair{
		HttpServletRequest request;
		HttpServletResponse response;
		
		
		public RequestResponsePair(HttpServletRequest request, HttpServletResponse response) {
			super();
			this.request = request;
			this.response = response;
		}
		
	}
}
