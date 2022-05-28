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
package com.mendmix.gateway.filter.post;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

import com.mendmix.common.CustomRequestHeaders;
import com.mendmix.common.model.ApiInfo;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.gateway.filter.PostFilterHandler;
import com.mendmix.gateway.helper.RuequestHelper;
import com.mendmix.gateway.model.BizSystemModule;
import com.mendmix.logging.integrate.ActionLog;
import com.mendmix.logging.integrate.ActionLogCollector;

/**
 * 
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年1月20日
 */
public class ResponseLogHandler implements PostFilterHandler {
	
	private boolean bodyIgnore = ResourceUtils.getBoolean("actionlog.responseBody.ignore", true);
	
	@Override
	public String process(ServerWebExchange exchange, BizSystemModule module,String respBodyAsString) {
		
		if(!exchange.getResponse().getStatusCode().is2xxSuccessful()) {
			return respBodyAsString;
		}
		
		ActionLog actionLog = exchange.getAttribute(ActionLogCollector.CURRENT_LOG_CONTEXT_NAME);
		if(actionLog == null)return respBodyAsString;
		
		HttpHeaders headers = exchange.getResponse().getHeaders();
		if(headers.containsKey(CustomRequestHeaders.HEADER_EXCEPTION_CODE)) {
			actionLog.setResponseCode(Integer.parseInt(headers.getFirst(CustomRequestHeaders.HEADER_EXCEPTION_CODE)));
		}
		
		if(actionLog.getResponseCode() != 200) {
			actionLog.setResponseCode(200);
		}
		
		if(bodyIgnore)return respBodyAsString;
		
		ApiInfo apiInfo = RuequestHelper.getCurrentApi(exchange);
		if(apiInfo != null && !apiInfo.isResponseLog()) {
        	return respBodyAsString;
        }
		//
		actionLog.setResponseData(respBodyAsString);
		
		return respBodyAsString;
	}

	@Override
	public int order() {
		return 0;
	}

}
