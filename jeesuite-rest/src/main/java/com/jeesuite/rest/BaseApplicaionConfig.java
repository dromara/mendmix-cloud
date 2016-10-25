/**
 * 
 */
package com.jeesuite.rest;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.web.filter.RequestContextFilter;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.jeesuite.rest.excetion.BaseExceptionMapper;
import com.jeesuite.rest.filter.format.FormatJsonDynamicFeature;
import com.jeesuite.rest.resolver.ObjectMapperResolver;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年1月16日
 * @Copyright (c) 2015, vakinge@github
 */
public class BaseApplicaionConfig extends ResourceConfig{

	public BaseApplicaionConfig() {
		//设置默认时区
		System.setProperty("user.timezone","Asia/Shanghai");
				
		this.packages(this.getClass().getPackage().getName());
		register(ObjectMapperResolver.class);
		register(JacksonFeature.class);
		register(JacksonJsonProvider.class);
		register(BaseExceptionMapper.class);
		
		register(RequestContextFilter.class);
		
		
//		register(GlobalFilter.class);
//		
//		register(AuthCheckFilter.class);
//		register(RequestLogFilter.class);
		register(FormatJsonDynamicFeature.class);
//		
//		//跨域
//		register(CorsFilter.class);
	}

	
}
