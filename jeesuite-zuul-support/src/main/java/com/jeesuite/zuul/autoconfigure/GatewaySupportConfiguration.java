package com.jeesuite.zuul.autoconfigure;

import java.text.SimpleDateFormat;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
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
import com.jeesuite.springweb.advice.ResonseRewriteAdvice;
import com.jeesuite.springweb.client.SimpleRestTemplateBuilder;
import com.jeesuite.springweb.exception.GlobalExceptionHandler;

@Configuration
@ConditionalOnMissingBean(GlobalExceptionHandler.class)
public class GatewaySupportConfiguration {

	@Bean("restTemplate")
	@LoadBalanced
	RestTemplate restTemplate() {
		int readTimeout = ResourceUtils.getInt("jeesuite.httpclient.readTimeout.ms", 30000);
		return SimpleRestTemplateBuilder.build(readTimeout);
	}

	
	@Bean
	public GlobalExceptionHandler globalExceptionHandler() {
		return new GlobalExceptionHandler();
	}
	
	@Bean
	@ConditionalOnProperty(value = "jeesuite.response.rewrite.enabled",havingValue = "true",matchIfMissing = true)
	public ResonseRewriteAdvice resonseRewriteAdvice() {
		return new ResonseRewriteAdvice();
	}
	
	@Bean
	public GlobalInternalScheduleService GlobalInternalScheduleService() {
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
