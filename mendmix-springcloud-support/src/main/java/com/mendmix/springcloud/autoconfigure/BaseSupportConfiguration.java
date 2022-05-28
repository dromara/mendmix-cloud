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
package com.mendmix.springcloud.autoconfigure;

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

import com.mendmix.common.util.ResourceUtils;
import com.mendmix.common2.task.GlobalInternalScheduleService;
import com.mendmix.springcloud.autoconfigure.loadbalancer.CustomBlockingLoadBalancerClient;
import com.mendmix.springweb.client.LoadBalancerWrapper;
import com.mendmix.springweb.client.SimpleRestTemplateBuilder;
import com.mendmix.springweb.enhancer.ResonseBodyEnhancerAdvice;
import com.mendmix.springweb.exception.GlobalExceptionHandler;

@Configuration
public class BaseSupportConfiguration {

	@Bean("restTemplate")
	@LoadBalanced
	RestTemplate restTemplate() {
		int readTimeout = ResourceUtils.getInt("mendmix.httpclient.readTimeout.ms", 30000);
		return SimpleRestTemplateBuilder.build(readTimeout);
	}
	
	@Bean
	@ConditionalOnProperty(value = "mendmix.loadbalancer.customize.enabled",havingValue = "true")
	public LoadBalancerClient blockingLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory) {
		return new CustomBlockingLoadBalancerClient(loadBalancerClientFactory);
	}
	
	@Bean
	@ConditionalOnWebApplication(type = Type.SERVLET)
	public GlobalExceptionHandler globalExceptionHandler() {
		return new GlobalExceptionHandler();
	}
	
	@Bean
	@ConditionalOnProperty(value = "mendmix.response.rewrite.enabled",havingValue = "true",matchIfMissing = true)
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
