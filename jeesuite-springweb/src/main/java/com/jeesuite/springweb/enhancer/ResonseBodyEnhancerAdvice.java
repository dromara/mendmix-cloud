package com.jeesuite.springweb.enhancer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.model.WrapperResponse;
import com.jeesuite.common.util.JsonUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.spring.InstanceFactory;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ResonseBodyEnhancerAdvice implements ResponseBodyAdvice<Object>,InitializingBean {

	private static List<ResponseBodyEnhancer> enhancers = new ArrayList<>();
	
	public static void register(ResponseBodyEnhancer enhancer) {
		enhancers.add(enhancer);
		if(enhancers.size() > 1) {			
			enhancers.stream().sorted(Comparator.comparing(ResponseBodyEnhancer::order));
		}
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if(ResourceUtils.getBoolean("jeesuite.response.rewrite.enabled", true)) {
			register(new ResponseRewrite());
		}
		Map<String, ResponseBodyEnhancer> beans = InstanceFactory.getBeansOfType(ResponseBodyEnhancer.class);
		for (ResponseBodyEnhancer enhancer : beans.values()) {
			register(enhancer);
		}
	}
	
    @Override
    public boolean supports(MethodParameter methodParameter,
                            Class<? extends HttpMessageConverter<?>> aClass) {
        return !enhancers.isEmpty(); // methodParameter.hasMethodAnnotation(ResponseBody.class);
    }


    @Override
    public Object beforeBodyWrite(Object body,
    		MethodParameter methodParameter,
            MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> aClass,
            ServerHttpRequest request,
            ServerHttpResponse response) {
       
    	for (ResponseBodyEnhancer enhancer : enhancers) {
			body = enhancer.process(body, methodParameter, mediaType, aClass,request,response);
		}
   
		return body;
    }

    private class ResponseRewrite implements ResponseBodyEnhancer {

		@Override
		public Object process(Object body,
	    		MethodParameter methodParameter,
	            MediaType mediaType,
	            Class<? extends HttpMessageConverter<?>> aClass,
	            ServerHttpRequest request,
	            ServerHttpResponse response) {
			
			if(response.getHeaders().containsKey(CustomRequestHeaders.HEADER_RESP_KEEP)) {
	        	return body;
	        }
			
			if(request.getHeaders().containsKey(CustomRequestHeaders.HEADER_RESP_KEEP)) {
	        	return body;
	        }
	    	
	    	if(body instanceof WrapperResponse) {
	        	return body;
	        }
	    	
	    	if(!mediaType.includes(MediaType.APPLICATION_JSON) && !methodParameter.hasMethodAnnotation(ResponseBody.class)) {
	    		return body;
	        }
	    	//
	    	response.getHeaders().add(CustomRequestHeaders.HEADER_RESP_KEEP, Boolean.TRUE.toString());
	    	WrapperResponse<Object> rewriteBody = new WrapperResponse<Object>(body);
	    	//
	    	if(StringHttpMessageConverter.class == aClass) {
	    		response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
	    		return JsonUtils.toJson(rewriteBody);
	    	}
	    	
			return rewriteBody;
		}

		@Override
		public int order() {
			return 99;
		}
    	
    }

}

