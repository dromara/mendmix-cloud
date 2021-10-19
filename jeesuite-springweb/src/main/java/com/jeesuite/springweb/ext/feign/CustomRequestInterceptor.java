package com.jeesuite.springweb.ext.feign;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import com.jeesuite.common.WebConstants;
import com.jeesuite.springweb.client.RequestHeaderBuilder;

import feign.RequestInterceptor;
import feign.RequestTemplate;

@Configuration
@ConditionalOnClass(feign.RequestInterceptor.class)
public class CustomRequestInterceptor implements RequestInterceptor {

	@Override
	public void apply(RequestTemplate template) {
		Map<String, String> customHeaders = RequestHeaderBuilder.getHeaders();
		customHeaders.forEach((k,v)->{					
			template.header(k, v);
		});  
		//
		template.header(WebConstants.HEADER_HTTP_STATUS_KEEP, Boolean.TRUE.toString());
	}

}
