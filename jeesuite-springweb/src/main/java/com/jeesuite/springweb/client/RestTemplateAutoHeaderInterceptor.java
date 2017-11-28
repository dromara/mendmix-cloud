package com.jeesuite.springweb.client;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.springweb.WebConstants;
import com.jeesuite.springweb.utils.IpUtils;

public class RestTemplateAutoHeaderInterceptor implements ClientHttpRequestInterceptor {

	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		try {
			Map<String, String> customHeaders = getCustomHeaders();
			request.getHeaders().setAll(customHeaders);
		} catch (Exception e) {}
		return execution.execute(request, body);
	}
	
	
	private static Map<String, String> getCustomHeaders(){
		Map<String, String> headers = new HashMap<>();
		 HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		Enumeration<String> headerNames = request.getHeaderNames();
		 while(headerNames.hasMoreElements()){
			 String headerName = headerNames.nextElement().toLowerCase();
			 if(headerName.startsWith(WebConstants.HEADER_PREFIX)){				 
				 String headerValue = request.getHeader(headerName);
				 if(headerValue != null)headers.put(headerName, headerValue);
			 }
		 }
		 //
		 headers.put(WebConstants.HEADER_INVOKER_IP, IpUtils.getLocalIpAddr());
		 
		 if(!headers.containsKey(WebConstants.HEADER_AUTH_TOKEN)){			 
			 headers.put(WebConstants.HEADER_AUTH_TOKEN, TokenGenerator.generateWithSign());
		 }
		 
		 return headers;
	}

}
