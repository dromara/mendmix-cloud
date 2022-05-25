package com.jeesuite.springboot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common2.task.GlobalInternalScheduleService;
import com.jeesuite.springboot.autoconfigure.loadbalancer.CustomBlockingLoadBalancerClient;
import com.jeesuite.springweb.client.LoadBalancerWrapper;
import com.jeesuite.springweb.client.SimpleRestTemplateBuilder;
import com.jeesuite.springweb.enhancer.ResonseBodyEnhancerAdvice;
import com.jeesuite.springweb.exception.GlobalExceptionHandler;

@Configuration
public class BaseSupportConfiguration {

	@Bean("restTemplate")
	@LoadBalanced
	RestTemplate restTemplate() {
		int readTimeout = ResourceUtils.getInt("jeesuite.httpclient.readTimeout.ms", 30000);
		return SimpleRestTemplateBuilder.build(readTimeout);
	}
	
	@Bean
	@ConditionalOnProperty(value = "jeesuite.loadbalancer.customize.enabled",havingValue = "true")
	public LoadBalancerClient blockingLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory) {
		return new CustomBlockingLoadBalancerClient(loadBalancerClientFactory);
	}
	
	@Bean
	@ConditionalOnWebApplication(type = Type.SERVLET)
	public GlobalExceptionHandler globalExceptionHandler() {
		return new GlobalExceptionHandler();
	}
	
	@Bean
	@ConditionalOnProperty(value = "jeesuite.response.rewrite.enabled",havingValue = "true",matchIfMissing = true)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	public ResonseBodyEnhancerAdvice resonseRewriteAdvice() {
		return new ResonseBodyEnhancerAdvice();
	}
	
	@Bean
	public GlobalInternalScheduleService globalInternalScheduleService() {
		return new GlobalInternalScheduleService();
	}
	
	@Bean
	public LoadBalancerWrapper loadBalancerWrapper(DiscoveryClient discoveryClient) {
		return new LoadBalancerWrapper(discoveryClient);
	}

}
