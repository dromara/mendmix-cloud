/*
 * Copyright 2016-2022 www.jeesuite.com.
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
package com.jeesuite.security;

import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.ThreadLocalContext;
import com.jeesuite.common.exception.ForbiddenAccessException;
import com.jeesuite.common.exception.UnauthorizedException;
import com.jeesuite.common.model.WrapperResponse;
import com.jeesuite.common.util.JsonUtils;
import com.jeesuite.common.util.PathMatcher;
import com.jeesuite.security.context.ReactiveRequestContextAdapter;
import com.jeesuite.security.model.UserSession;

import reactor.core.publisher.Mono;

/**
 * 
 * <br>
 * Class Name   : ReactiveFilter
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Apr 5, 2022
 */
public class ReactiveSecurityDelegatingFilter implements WebFilter {

	private static final String XML_HTTP_REQUEST = "XMLHttpRequest";
	
	private PathMatcher pathMatcher;
	private ReactiveCustomAuthnHandler customAuthnHandler;
	
	public ReactiveSecurityDelegatingFilter(ReactiveCustomAuthnHandler customAuthnHandler,String...uriPatterns) {
		this.customAuthnHandler = customAuthnHandler;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		if(!pathMatcher.match(request.getPath().value())) {
			return chain.filter(exchange);
		}
		
		if(request.getMethod().equals(HttpMethod.OPTIONS)) {
			return chain.filter(exchange);
		}
		
		exchange.getAttributes().clear();
		ReactiveRequestContextAdapter.init(request);
		
		if(customAuthnHandler != null) {
			customAuthnHandler.beforeAuthentication(exchange);
		}
		
		ServerHttpResponse response = exchange.getResponse();
		
		UserSession userSession = null;
		try {
			if(customAuthnHandler == null || !customAuthnHandler.customAuthentication(exchange)) {
				userSession = SecurityDelegating.doAuthorization(request.getMethodValue(),request.getPath().value());
			}
		} catch (UnauthorizedException e) {
			if(isAjax(request) || SecurityDelegating.getConfigurerProvider().error401Page() == null){	
				byte[] bytes = JsonUtils.toJsonBytes(WrapperResponse.buildErrorResponse(e));
				return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
			}else{
				response.getHeaders().setLocation(URI.create(SecurityDelegating.getConfigurerProvider().error401Page()));
				return chain.filter(exchange);
			}
		}catch (ForbiddenAccessException e) {
			if(isAjax(request) || SecurityDelegating.getConfigurerProvider().error403Page() == null){				
				byte[] bytes = JsonUtils.toJsonBytes(WrapperResponse.buildErrorResponse(e));
				return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
			}else{
				response.getHeaders().setLocation(URI.create(SecurityDelegating.getConfigurerProvider().error403Page()));
				return chain.filter(exchange);
			}
		}
		//
		if(customAuthnHandler != null) {
			customAuthnHandler.afterAuthentication(exchange,userSession);
		}

		try {
			return chain.filter(exchange) //
				    .doFinally(s -> {
					   exchange.getAttributes().clear();
				    });
		} finally {
			ThreadLocalContext.unset();
		}
	}
	
	private static boolean isAjax(ServerHttpRequest request){
	    return  (request.getHeaders().containsKey(CustomRequestHeaders.HEADER_REQUESTED_WITH)  
	    && XML_HTTP_REQUEST.equalsIgnoreCase(request.getHeaders().getFirst(CustomRequestHeaders.HEADER_REQUESTED_WITH).toString())) ;
	}

}
