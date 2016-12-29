/**
 * 
 */
package com.jeesuite.rest;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.web.filter.RequestContextFilter;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.jeesuite.rest.excetion.BaseExceptionMapper;
import com.jeesuite.rest.filter.DefaultWebFilter;
import com.jeesuite.rest.resolver.ObjectMapperResolver;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年1月16日
 * @Copyright (c) 2015, vakinge@github
 */
public abstract class BaseApplicaionConfig extends ResourceConfig implements CustomConfig {

	public BaseApplicaionConfig() {
		//设置默认时区
		System.setProperty("user.timezone","Asia/Shanghai");
				
		this.packages(packages());
		register(ObjectMapperResolver.class);
		register(JacksonFeature.class);
		register(JacksonJsonProvider.class);
		register(new BaseExceptionMapper(createExcetionWrapper()));
		register(RequestContextFilter.class);
		
		register(DefaultWebFilter.class);
	}

	
}
