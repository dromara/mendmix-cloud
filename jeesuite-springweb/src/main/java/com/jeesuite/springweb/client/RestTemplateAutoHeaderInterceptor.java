package com.jeesuite.springweb.client;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class RestTemplateAutoHeaderInterceptor implements ClientHttpRequestInterceptor {

	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		try {
			Map<String, String> customHeaders = RequestHeaderBuilder.getHeaders();
			request.getHeaders().setAll(customHeaders);
		} catch (Exception e) {}
		return execution.execute(request, body);
	}
	
	
	

}
