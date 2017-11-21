package com.jeesuite.springweb.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class SimpleRestTemplateBuilder {

	public RestTemplate build(){
		
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();  
        factory.setReadTimeout(15000);//ms  
        factory.setConnectTimeout(10000);//ms 
        
        RestTemplate restTemplate = new RestTemplate(factory);
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new RestTemplateAutoHeaderInterceptor());
        restTemplate.setInterceptors(interceptors);
        //
        restTemplate.setErrorHandler(new CustomResponseErrorHandler());
		return restTemplate;
	}
}
