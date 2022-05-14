package com.jeesuite.springboot.autoconfigure;

import java.text.SimpleDateFormat;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common2.task.GlobalInternalScheduleService;
import com.jeesuite.springboot.autoconfigure.feign.CustomLoadBalancerFeignClient;
import com.jeesuite.springweb.client.SimpleRestTemplateBuilder;
import com.jeesuite.springweb.enhancer.ResonseBodyEnhancerAdvice;
import com.jeesuite.springweb.exception.GlobalExceptionHandler;

import feign.Client;

@Configuration
public class BaseSupportConfiguration {

	@Bean("restTemplate")
	@LoadBalanced
	RestTemplate restTemplate() {
		int readTimeout = ResourceUtils.getInt("jeesuite.httpclient.readTimeout.ms", 30000);
		return SimpleRestTemplateBuilder.build(readTimeout);
	}
	
	@Bean
	@ConditionalOnProperty(value = "jeesuite.feign.proxy.enabled",havingValue = "true")
	public Client feignClient(LoadBalancerClient loadBalancer) {
		return new CustomLoadBalancerFeignClient(loadBalancer);
	}
	
	@Bean
	public GlobalExceptionHandler globalExceptionHandler() {
		return new GlobalExceptionHandler();
	}
	
	@Bean
	@ConditionalOnProperty(value = "jeesuite.response.rewrite.enabled",havingValue = "true",matchIfMissing = true)
	public ResonseBodyEnhancerAdvice resonseRewriteAdvice() {
		return new ResonseBodyEnhancerAdvice();
	}
	
	@Bean
	public GlobalInternalScheduleService globalInternalScheduleService() {
		return new GlobalInternalScheduleService();
	}
	
	@Bean
    @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }
}