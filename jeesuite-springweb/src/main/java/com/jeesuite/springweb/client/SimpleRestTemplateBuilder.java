package com.jeesuite.springweb.client;

import java.util.List;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class SimpleRestTemplateBuilder {

	public RestTemplate build(List<ClientHttpRequestInterceptor> interceptors){
		
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();  
        factory.setReadTimeout(10000);//ms  
        factory.setConnectTimeout(10000);//ms 
        
        RestTemplate restTemplate = new RestTemplate(factory);
        interceptors.add(new RestTemplateAutoHeaderInterceptor());
        restTemplate.setInterceptors(interceptors);
        //
        restTemplate.setErrorHandler(new CustomResponseErrorHandler());
		return restTemplate;
	}
}
