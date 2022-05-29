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
package com.mendmix.security;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.mendmix.common.CustomRequestHeaders;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.exception.ForbiddenAccessException;
import com.mendmix.common.exception.UnauthorizedException;
import com.mendmix.common.model.WrapperResponse;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.security.context.ReactiveRequestContextAdapter;
import com.mendmix.security.model.UserSession;

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

	private static Logger logger = LoggerFactory.getLogger("com.mendmix.security");
	
	private static final String XML_HTTP_REQUEST = "XMLHttpRequest";
	
	private List<String> matchUriPrefixs;
	private String matchUriPrefix;
	private ReactiveCustomAuthnHandler customAuthnHandler;
	
	public ReactiveSecurityDelegatingFilter(ReactiveCustomAuthnHandler customAuthnHandler,String...matchUriPrefixs) {
		this.customAuthnHandler = customAuthnHandler;
		if(matchUriPrefixs.length > 1) {
			this.matchUriPrefixs = Arrays.asList(matchUriPrefixs);
		}else {
			this.matchUriPrefix = matchUriPrefixs[0];
		}
		
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		try {
			if((matchUriPrefix != null && !request.getPath().value().startsWith(matchUriPrefix)) 
					|| (matchUriPrefixs != null && !matchUriPrefixs.stream().anyMatch(o -> request.getPath().value().startsWith(o)))) {
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
				if(isAjax(request) || SecurityDelegating.decisionProvider().error401Page() == null){	
					byte[] bytes = JsonUtils.toJsonBytes(WrapperResponse.fail(e));
					return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
				}else{
					response.getHeaders().setLocation(URI.create(SecurityDelegating.decisionProvider().error401Page()));
					return chain.filter(exchange);
				}
			}catch (ForbiddenAccessException e) {
				if(isAjax(request) || SecurityDelegating.decisionProvider().error403Page() == null){				
					byte[] bytes = JsonUtils.toJsonBytes(WrapperResponse.fail(e));
					return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
				}else{
					response.getHeaders().setLocation(URI.create(SecurityDelegating.decisionProvider().error403Page()));
					return chain.filter(exchange);
				}
			}
			//
			if(customAuthnHandler != null) {
				customAuthnHandler.afterAuthentication(exchange,userSession);
			}
			return chain.filter(exchange) //
				    .doFinally(s -> {
					   exchange.getAttributes().clear();
				    });
		} catch (Exception e) {
			logger.error("_global_filter_error",e);
			ThreadLocalContext.unset();
			exchange.getAttributes().clear();
			byte[] bytes = JsonUtils.toJsonBytes(WrapperResponse.fail(e));
			return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
		} 
	}
	
	private static boolean isAjax(ServerHttpRequest request){
	    return  (request.getHeaders().containsKey(CustomRequestHeaders.HEADER_REQUESTED_WITH)  
	    && XML_HTTP_REQUEST.equalsIgnoreCase(request.getHeaders().getFirst(CustomRequestHeaders.HEADER_REQUESTED_WITH).toString())) ;
	}

}
