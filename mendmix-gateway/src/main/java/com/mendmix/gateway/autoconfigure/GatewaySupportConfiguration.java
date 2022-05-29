/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.gateway.autoconfigure;

import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import com.mendmix.gateway.endpoint.ActuatorController;
import com.mendmix.gateway.endpoint.ServiceExporterController;
import com.mendmix.gateway.exception.ReactiveGlobalExceptionHandler;
import com.mendmix.gateway.exception.RouteErrorWebExceptionHandler;
import com.mendmix.gateway.router.CustomRouteDefinitionRepository;

@Configuration
public class GatewaySupportConfiguration {

	@Bean
	public ReactiveGlobalExceptionHandler globalExceptionHandler() {
		return new ReactiveGlobalExceptionHandler();
	}
	
	@Bean
	public CustomRouteDefinitionRepository customRouteDefinitionRepository() {
		return new CustomRouteDefinitionRepository();
	}
	
	@Bean
	public ActuatorController actuatorController() {
		return new ActuatorController();
	}
	
	@Bean
	public ServiceExporterController serviceExporterController() {
		return new ServiceExporterController();
	}
	
	@Bean
	public ErrorWebExceptionHandler errorWebExceptionHandler(
			      ErrorAttributes errorAttributes,
			      WebProperties webProperties, 
			      ObjectProvider<ViewResolver> viewResolvers,
			      ServerCodecConfigurer serverCodecConfigurer, 
			      ServerProperties serverProperties,
			      ApplicationContext applicationContext
		) {
		RouteErrorWebExceptionHandler exceptionHandler = new RouteErrorWebExceptionHandler(errorAttributes,
				webProperties.getResources(), serverProperties.getError(), applicationContext);
		exceptionHandler.setViewResolvers(viewResolvers.orderedStream().collect(Collectors.toList()));
		exceptionHandler.setMessageWriters(serverCodecConfigurer.getWriters());
		exceptionHandler.setMessageReaders(serverCodecConfigurer.getReaders());
		return exceptionHandler;
	}

}
