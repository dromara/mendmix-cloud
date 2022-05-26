package com.jeesuite.gateway.autoconfigure;

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

import com.jeesuite.gateway.endpoint.ActuatorController;
import com.jeesuite.gateway.exception.ReactiveGlobalExceptionHandler;
import com.jeesuite.gateway.exception.RouteErrorWebExceptionHandler;
import com.jeesuite.gateway.router.CustomRouteDefinitionRepository;

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
