/**
 * 
 */
package com.jeesuite.rest;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.springframework.web.filter.RequestContextFilter;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.jeesuite.rest.excetion.BaseExceptionMapper;
import com.jeesuite.rest.filter.DefaultWebFilter;
import com.jeesuite.rest.resolver.ObjectMapperResolver;
import com.jeesuite.rest.resolver.ValidationContextResolver;

import io.swagger.jaxrs.listing.SwaggerSerializers;

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
		
		register(ValidationContextResolver.class);
		property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
	    property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
				
		this.packages(packages());
		register(SwaggerSerializers.class);
		register(ObjectMapperResolver.class);
		register(JacksonFeature.class);
		register(JacksonJsonProvider.class);
		register(new BaseExceptionMapper(createExcetionWrapper()));
		register(RequestContextFilter.class);
		
		register(DefaultWebFilter.class);
	}

	
}
