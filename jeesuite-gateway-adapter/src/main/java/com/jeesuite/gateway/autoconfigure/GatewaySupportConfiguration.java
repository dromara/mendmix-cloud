package com.jeesuite.gateway.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.jeesuite.common2.task.GlobalInternalScheduleService;
import com.jeesuite.springweb.AppConfigs;
import com.jeesuite.springweb.client.SimpleRestTemplateBuilder;
import com.jeesuite.springweb.enhancer.ResonseBodyEnhancerAdvice;
import com.jeesuite.springweb.exception.GlobalExceptionHandler;

@Configuration
@ConditionalOnMissingBean(GlobalExceptionHandler.class)
public class GatewaySupportConfiguration {

	@Bean("restTemplate")
	@LoadBalanced
	RestTemplate restTemplate() {
		return SimpleRestTemplateBuilder.build(AppConfigs.readTimeout);
	}

	
	@Bean
	public GlobalExceptionHandler globalExceptionHandler() {
		return new GlobalExceptionHandler();
	}
	
	@Bean
	public ResonseBodyEnhancerAdvice resonseBodyEnhancerAdvice() {
		return new ResonseBodyEnhancerAdvice();
	}
	
	@Bean
	public GlobalInternalScheduleService globalInternalScheduleService() {
		return new GlobalInternalScheduleService();
	}
	
}
