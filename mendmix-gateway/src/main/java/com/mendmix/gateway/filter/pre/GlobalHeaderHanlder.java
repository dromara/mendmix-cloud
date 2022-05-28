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

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.web.server.ServerWebExchange;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.CustomRequestHeaders;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.util.TokenGenerator;
import com.mendmix.gateway.GatewayConstants;
import com.mendmix.gateway.filter.PreFilterHandler;
import com.mendmix.gateway.model.BizSystemModule;
import com.mendmix.springweb.client.RequestHeaderBuilder;


/**
 * 
 * 
 * <br>
 * Class Name   : GlobalHeaderHanlder
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2020年9月15日
 */
public class GlobalHeaderHanlder implements PreFilterHandler {

	@Override
	public Builder process(ServerWebExchange exchange,BizSystemModule module,Builder requestBuilder) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		Builder reqBuilder = requestBuilder == null ? exchange.getRequest().mutate() : requestBuilder;
		if(!headers.containsKey(CustomRequestHeaders.HEADER_REQUEST_ID)){
			reqBuilder.header(CustomRequestHeaders.HEADER_REQUEST_ID, TokenGenerator.generate());
		}
		reqBuilder.header(CustomRequestHeaders.HEADER_INVOKER_IS_GATEWAY,Boolean.TRUE.toString());
		//
		RequestHeaderBuilder.getHeaders().forEach( (k,v) -> {
			reqBuilder.header(k,v);
		} );
		
		//
		Boolean trustedRequest = ThreadLocalContext.get(GatewayConstants.CONTEXT_TRUSTED_REQUEST);
		if(trustedRequest == null || !trustedRequest) {
			//一些token禁止前端传入
			if(headers.containsKey(CustomRequestHeaders.HEADER_IGNORE_TENANT)) {
				reqBuilder.headers(httpHeaders -> httpHeaders.remove(CustomRequestHeaders.HEADER_IGNORE_TENANT));
			}
			if(headers.containsKey(CustomRequestHeaders.HEADER_AUTH_USER)) {
				reqBuilder.headers(httpHeaders -> httpHeaders.remove(CustomRequestHeaders.HEADER_AUTH_USER));
				AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
				if(currentUser != null) {
					reqBuilder.header(CustomRequestHeaders.HEADER_AUTH_USER, currentUser.toEncodeString());
				}
			}
		}
		
		return reqBuilder;
	}

	@Override
	public int order() {
		return 1;
	}

}
