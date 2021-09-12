package com.jeesuite.springweb.configure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.jeesuite.common.WebConstants;
import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.springweb.model.WrapperResponse;

@Configuration
@ControllerAdvice
@ConditionalOnProperty(name = "response.reWrite.enbaled",havingValue = "true",matchIfMissing = true)
public class ResonseRewriteConfiguration implements ResponseBodyAdvice<Object>{

	private boolean responseBodyForce = ResourceUtils.getBoolean("response.reWrite.responseBody.force");
    @Override
    public boolean supports(MethodParameter methodParameter,
                            Class<? extends HttpMessageConverter<?>> aClass) {
        return true; // methodParameter.hasMethodAnnotation(ResponseBody.class);
    }


    @Override
    public Object beforeBodyWrite(Object body,
    		MethodParameter methodParameter,
            MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> aClass,
            ServerHttpRequest request,
            ServerHttpResponse response) {
       
    	
    	if(request.getHeaders().containsKey(WebConstants.HEADER_RESP_KEEP)) {
        	return body;
        }
    	
    	if(response.getHeaders().containsKey(WebConstants.HEADER_RESP_KEEP)) {
        	return body;
        }
    	
    	if(body instanceof WrapperResponse) {
        	return body;
        }
    	
        if(responseBodyForce) {
        	if(!mediaType.includes(MediaType.APPLICATION_JSON) && !methodParameter.hasMethodAnnotation(ResponseBody.class)) {
        		return body;
            }
    	}else if(!mediaType.includes(MediaType.APPLICATION_JSON)) {
    		return body;
        }
    	//
    	response.getHeaders().add(WebConstants.HEADER_RESP_KEEP, Boolean.TRUE.toString());
    	
    	WrapperResponse<Object> rewriteBody = new WrapperResponse<Object>(body);
    	//
    	if(responseBodyForce && StringHttpMessageConverter.class == aClass) {
    		response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    		return JsonUtils.toJson(rewriteBody);
    	}
    	
		return rewriteBody;
    }


}

