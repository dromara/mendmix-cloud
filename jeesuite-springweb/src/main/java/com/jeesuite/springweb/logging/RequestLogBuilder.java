package com.jeesuite.springweb.logging;

import java.util.Arrays;

import com.jeesuite.common.util.JsonUtils;

public class RequestLogBuilder {

	public static String requestLogMessage(String uri,String method,Object parameters,Object body) {
		StringBuilder builder = new StringBuilder();
		
    	builder.append("\n-----------request start-----------\n");
    	builder.append("uri      :").append(uri).append("\n");
    	builder.append("method   :").append(method).append("\n");
    	if(parameters != null) {
    		builder.append("parameters  :").append(parameters).append("\n");
    	}
    	
    	if(body != null) {
    		String bodyString;
    		if(body instanceof byte[]) {
    			byte[] bodyBytes = (byte[])body;
    			if(bodyBytes.length > 1024)bodyBytes = Arrays.copyOf(bodyBytes, 1024);
    			bodyString = new String(bodyBytes);
    		}else if(body instanceof String) {
    			bodyString = body.toString();
    		}else {
    			bodyString = JsonUtils.toJson(body);
    		}
    		builder.append("body  :").append(bodyString).append("\n");
    	}
    	builder.append("-----------request end-----------\n");
    	
    	return builder.toString();
	}
	
	
	public static String responseLogMessage(int statusCode,Object body) {
		StringBuilder builder = new StringBuilder();
    	builder.append("\n-----------response start-----------\n");
    	builder.append("statusCode      :").append(statusCode).append("\n");
    	if(body != null) {
    		String bodyString;
    		if(body instanceof byte[]) {
    			byte[] bodyBytes = (byte[])body;
    			if(bodyBytes.length > 1024)bodyBytes = Arrays.copyOf(bodyBytes, 1024);
    			bodyString = new String(bodyBytes);
    		}else if(body instanceof String) {
    			bodyString = body.toString();
    		}else {
    			bodyString = JsonUtils.toJson(body);
    		}
    		builder.append("body  :").append(bodyString).append("\n");
    	}
    	
    	builder.append("-----------response end-----------\n");
    	return builder.toString();
	}
    

}
