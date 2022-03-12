package com.jeesuite.springweb.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class SimpleRestTemplateBuilder {

	
	public static RestTemplate build(){
		return build(30000);
	}
	
	public static RestTemplate build(int readTimeout){
		
		
		ClientHttpRequestFactory factory = null;
		try {
			Class.forName("okhttp3.OkHttpClient");
			OkHttp3ClientHttpRequestFactory _factory = new OkHttp3ClientHttpRequestFactory();  
			_factory.setConnectTimeout(3000);
			_factory.setReadTimeout(readTimeout);
			_factory.setWriteTimeout(readTimeout);
			factory = _factory;
		} catch (ClassNotFoundException e) {}
		if(factory == null) {
			try {
				Class.forName("org.apache.http.client.HttpClient");
				HttpComponentsClientHttpRequestFactory _factory = new HttpComponentsClientHttpRequestFactory();  
				_factory.setReadTimeout(readTimeout);//ms  
				_factory.setConnectTimeout(3000);//ms 
				factory = _factory;
			} catch (ClassNotFoundException e) {}
		}
		
		if(factory == null) {
			SimpleClientHttpRequestFactory _factory = new SimpleClientHttpRequestFactory();
			_factory.setConnectTimeout(3000);
			_factory.setReadTimeout(readTimeout);
			factory = _factory;
		}

        RestTemplate restTemplate = new RestTemplate(factory);
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new RestTemplateAutoHeaderInterceptor());
        interceptors.add(new HttpClientLoggingInterceptor());
        restTemplate.setInterceptors(interceptors);
        //
        restTemplate.setErrorHandler(new CustomResponseErrorHandler());
		return restTemplate;
	}
}
