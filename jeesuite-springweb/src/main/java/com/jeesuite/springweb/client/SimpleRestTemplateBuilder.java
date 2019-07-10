package com.jeesuite.springweb.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.jeesuite.springweb.interceptor.LoggingRequestInterceptor;

public class SimpleRestTemplateBuilder {

	
	public RestTemplate build(){
		return build(30000);
	}
	
	public RestTemplate build(int readTimeout){
		
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();  
        factory.setReadTimeout(readTimeout);//ms  
        factory.setConnectTimeout(3000);//ms 
        
        RestTemplate restTemplate = new RestTemplate(factory);
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new RestTemplateAutoHeaderInterceptor());
        interceptors.add(new LoggingRequestInterceptor());
        restTemplate.setInterceptors(interceptors);
        //
        restTemplate.setErrorHandler(new CustomResponseErrorHandler());
		return restTemplate;
	}
}
