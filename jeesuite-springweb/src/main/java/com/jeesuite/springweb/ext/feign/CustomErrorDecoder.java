package com.jeesuite.springweb.ext.feign;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import com.google.common.io.CharStreams;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.util.JsonUtils;

import feign.Response;
import feign.codec.ErrorDecoder;

@Configuration
@ConditionalOnClass(feign.RequestInterceptor.class)
public class CustomErrorDecoder implements ErrorDecoder {

	private static Logger logger = LoggerFactory.getLogger("com.jeesuite.core.rpc");
	
	@Override
	public Exception decode(String methodKey, Response response) {
		if(response.body() != null){
    		try {					
    			String content = CharStreams.toString(new InputStreamReader(response.body().asInputStream(), StandardCharsets.UTF_8));
    			Map responseBody = JsonUtils.toObject(content, Map.class);
    			if(responseBody.containsKey("code")){
    				int code = Integer.parseInt(responseBody.get("code").toString());
    				return new JeesuiteBaseException(code,Objects.toString(responseBody.get("msg")));
    			}
			} catch (Exception e) {}
    	}else {
    		logger.error("feign_client_error ->method:{},status:{},message:{}", methodKey,response.status(),response.reason());
			 String message = response.reason();
			 if(message == null)message = "服务调用错误";
			 return new JeesuiteBaseException(response.status(),message + "("+methodKey+")");
		}
        
		String error = String.format("feign_client_error ->method:%s,status:%s,message:%s", methodKey,response.status(),response.reason());
        return new JeesuiteBaseException(500,error);
	}

}
