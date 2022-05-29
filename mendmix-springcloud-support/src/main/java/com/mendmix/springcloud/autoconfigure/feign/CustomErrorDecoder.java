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
package com.mendmix.springcloud.autoconfigure.feign;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import com.google.common.io.CharStreams;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.util.JsonUtils;

import feign.Response;
import feign.codec.ErrorDecoder;

@Configuration
@ConditionalOnClass(feign.RequestInterceptor.class)
public class CustomErrorDecoder implements ErrorDecoder {

	private static Logger logger = LoggerFactory.getLogger("com.mendmix.rpc");
	
	@Override
	public Exception decode(String methodKey, Response response) {
		if(response.body() != null){
    		try {					
    			String content = CharStreams.toString(new InputStreamReader(response.body().asInputStream(), StandardCharsets.UTF_8));
    			Map responseBody = JsonUtils.toObject(content, Map.class);
    			if(responseBody.containsKey("code")){
    				int code = Integer.parseInt(responseBody.get("code").toString());
    				return new MendmixBaseException(code,Objects.toString(responseBody.get("msg")));
    			}
			} catch (Exception e) {}
    	}else {
    		logger.error("feign_client_error ->method:{},status:{},message:{}", methodKey,response.status(),response.reason());
			 String message = response.reason();
			 if(message == null)message = "服务调用错误";
			 return new MendmixBaseException(response.status(),message + "("+methodKey+")");
		}
        
		String error = String.format("feign_client_error ->method:%s,status:%s,message:%s", methodKey,response.status(),response.reason());
        return new MendmixBaseException(500,error);
	}

}
