/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.gateway.filter;

import java.util.List;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.exception.ForbiddenAccessException;
import org.dromara.mendmix.common.exception.UnauthorizedException;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.model.WrapperResponse;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.gateway.CurrentSystemHolder;
import org.dromara.mendmix.gateway.GatewayConstants;
import org.dromara.mendmix.gateway.helper.RequestContextHelper;
import org.dromara.mendmix.gateway.model.BizSystem;
import org.dromara.mendmix.gateway.model.BizSystemPortal;
import org.dromara.mendmix.gateway.security.AuthorizationProvider;
import org.dromara.mendmix.gateway.security.SpecUnauthorizedHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Jun 11, 2022
 */
public class GlobalFilter implements WebFilter {

	private static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.security");
	
	private String matchUriPrefix;
	private AuthorizationProvider authorizationProvider;
	private SpecUnauthorizedHandler specUnauthorizedHandler = new SpecUnauthorizedHandler();


	public GlobalFilter(String matchUriPrefix, AuthorizationProvider authorizationProvider) {
		this.matchUriPrefix = matchUriPrefix;
		this.authorizationProvider = authorizationProvider;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		try {
			String uri = RequestContextHelper.getOriginRequestUri(exchange);
			if((matchUriPrefix != null && !uri.startsWith(matchUriPrefix)) 
					|| RequestContextHelper.isWebSocketRequest(exchange.getRequest())) {
				exchange.getAttributes().put(GatewayConstants.CONTEXT_IGNORE_FILTER, Boolean.TRUE);
				return chain.filter(exchange);
			}
			
			if(request.getMethod().equals(HttpMethod.OPTIONS)) {
				return chain.filter(exchange);
			}
			
			RequestContextHelper.clearContextAttributes(exchange);
			
			beforeAuthentication(exchange);
			
			ServerHttpResponse response = exchange.getResponse();
			
			AuthUser currentUser = null;
			try {
				currentUser = authorizationProvider.doAuthorization(request);
			} catch (UnauthorizedException e) {
				if(!specUnauthorizedHandler.customAuthentication(exchange)) {
					RequestContextHelper.clearContextAttributes(exchange);
					return writeErrorResponse(request,response, e);
				}
				if(request.getHeaders().containsKey(CustomRequestHeaders.HEADER_AUTH_USER)) {
					AuthUser authUser = AuthUser.decode(request.getHeaders().getFirst(CustomRequestHeaders.HEADER_AUTH_USER));
					CurrentRuntimeContext.setAuthUser(authUser);
				}
			}catch (ForbiddenAccessException e) {	
				RequestContextHelper.clearContextAttributes(exchange);
				return writeErrorResponse(request,response, e);
			}
			//
			afterAuthentication(exchange,currentUser);
			
			return chain.filter(exchange) //
				    .doFinally(s -> {
				    	RequestContextHelper.clearContextAttributes(exchange);
				    });
		} catch (Exception e) {
			logger.error("MENDMIX-TRACE-LOGGGING-->> _global_filter_error",e);
			ThreadLocalContext.unset();
			exchange.getAttributes().clear();
			byte[] bytes = JsonUtils.toJsonBytes(WrapperResponse.fail(e));
			return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
		} 
	}

	
	
	private void beforeAuthentication(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		String domain = RequestContextHelper.getOriginDomain(request);
		BizSystemPortal portal = CurrentSystemHolder.getSystemPortal(domain);
		if(portal != null) {
			CurrentRuntimeContext.setTenantId(portal.getTenantId());
			CurrentRuntimeContext.setClientType(portal.getClientType());
		}
		//
		String systemId = getHeaderSystemId(request);
		if(systemId == null) {
			BizSystem system = CurrentSystemHolder.getSystem();
			if(system != null)systemId = system.getId();
		}
		if(systemId != null) {
			CurrentRuntimeContext.setSystemId(systemId);
		}
	}
	
	private String getHeaderSystemId(ServerHttpRequest request) {
		String systemId = request.getHeaders().getFirst(CustomRequestHeaders.HEADER_SYSTEM_ID);
		if(systemId != null) {
			if(logger.isTraceEnabled())logger.trace("header[x-system-id]={}",systemId);
			boolean matched = false;
			List<BizSystem> systems = CurrentSystemHolder.getSystems();
			for (BizSystem system : systems) {
				if(systemId.equals(system.getId()) || systemId.equals(system.getCode())) {
					systemId = system.getId();
					matched = true;
					break;
				}
			}
			if(!matched) {
				logger.warn("MENDMIX-TRACE-LOGGGING-->> header[x-system-id]={} can't matched",systemId);
				systemId = null;
			}
		
		}
		return systemId;
	}
	
	private void afterAuthentication(ServerWebExchange exchange,AuthUser currentUser) {
		
	}
	
	private Mono<Void> writeErrorResponse(ServerHttpRequest request,ServerHttpResponse response,MendmixBaseException e){
		if(GlobalConstants.FEIGN_USER_AGENT_NAME.equalsIgnoreCase(request.getHeaders().getFirst(HttpHeaders.USER_AGENT))) {
			response.setRawStatusCode(e.getCode());
		}
		response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
		byte[] bytes = JsonUtils.toJsonBytes(WrapperResponse.fail(e));
		return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
	}

}
