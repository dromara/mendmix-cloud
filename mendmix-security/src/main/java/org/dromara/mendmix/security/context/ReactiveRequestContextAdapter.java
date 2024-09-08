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
package org.dromara.mendmix.security.context;

import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.security.RequestContextAdapter;

import reactor.core.publisher.Mono;

/**
 * 
 * <br>
 * Class Name   : ServletRequestContextAdapter
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date May 14, 2022
 */
public class ReactiveRequestContextAdapter implements RequestContextAdapter {

	public static void init(ServerHttpRequest request) {
		ThreadLocalContext.set(_CTX_REQUEST_KEY, request);
		CurrentRuntimeContext.addContextHeaders(request.getHeaders().toSingleValueMap());		
	}
	
	@Override
	public String getHeader(String headerName) {
		ServerHttpRequest request = ThreadLocalContext.get(_CTX_REQUEST_KEY);	
		return request.getHeaders().getFirst(headerName);
	}

	@Override
	public String getCookie(String cookieName) {
		ServerHttpRequest request = ThreadLocalContext.get(_CTX_REQUEST_KEY);	
		HttpCookie cookie = request.getCookies().getFirst(cookieName);
		return cookie == null ? null : cookie.getValue();
	}

	@Override
	public void addCookie(String domain, String cookieName, String cookieValue, int expire) {
		
	}
	
	private static Mono<ServerHttpRequest> getServerHttpRequest() {
	    return Mono.deferContextual(Mono::just)
	        .map(contextView -> contextView.get(ServerWebExchange.class).getRequest());
	}

}
