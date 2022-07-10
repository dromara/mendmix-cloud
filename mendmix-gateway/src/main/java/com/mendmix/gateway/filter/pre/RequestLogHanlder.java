/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.gateway.filter.pre;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.web.server.ServerWebExchange;

import com.mendmix.common.model.ApiInfo;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.gateway.filter.PreFilterHandler;
import com.mendmix.gateway.helper.RequestContextHelper;
import com.mendmix.gateway.model.BizSystemModule;
import com.mendmix.logging.integrate.ActionLog;
import com.mendmix.logging.integrate.ActionLogCollector;


/**
 * 
 * 
 * <br>
 * Class Name   : RequestLogHanlder
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2020年9月15日
 */
public class RequestLogHanlder implements PreFilterHandler {

	@Override
	public Builder process(ServerWebExchange exchange,BizSystemModule module,Builder requestBuilder) {
		
		if(RequestContextHelper.isWebSocketRequest(exchange.getRequest())) {
    	   return requestBuilder;
    	}
		
		ActionLog actionLog = exchange.getAttribute(ActionLogCollector.CURRENT_LOG_CONTEXT_NAME);
		if(actionLog == null)return requestBuilder;
		actionLog.setModuleId(module.getServiceId());
		
		ServerHttpRequest request = exchange.getRequest();
		ApiInfo apiInfo = RequestContextHelper.getCurrentApi(exchange);
        if(apiInfo != null && !apiInfo.isRequestLog()) {
        	return requestBuilder;
        }
        actionLog.setQueryParameters(JsonUtils.toJson(request.getQueryParams()));
        if(HttpMethod.POST.equals(request.getMethod()) && !RequestContextHelper.isMultipartContent(request)) {
        	try {
        		String data = RequestContextHelper.getCachingBodyString(exchange);
        		actionLog.setRequestData(data);
        	} catch (Exception e) {
        		throw new RuntimeException(e); 
        	}
        	
        }

		return requestBuilder;
	}

	@Override
	public int order() {
		return 9;
	}


}
