package com.jeesuite.gateway.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jeesuite.gateway.exception.ReactiveGlobalExceptionHandler;
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

}
