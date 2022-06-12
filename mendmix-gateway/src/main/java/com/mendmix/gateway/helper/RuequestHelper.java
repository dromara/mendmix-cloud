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
package com.mendmix.gateway.helper;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import com.mendmix.common.GlobalConstants;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.model.ApiInfo;
import com.mendmix.common.util.DigestUtils;
import com.mendmix.common.util.IpUtils;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.ParameterUtils;
import com.mendmix.common.util.WebUtils;
import com.mendmix.gateway.CurrentSystemHolder;
import com.mendmix.gateway.GatewayConstants;
import com.mendmix.gateway.model.BizSystemModule;

/**
 * 
 * <br>
 * Class Name : RuequestHelper
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date May 14, 2022
 */
public class RuequestHelper {

	private static final String WEBSOCKET_KEYS = "sec-websocket-key";

	public static String getOriginDomain(ServerHttpRequest request) {
		String originUrl = request.getHeaders().getFirst(HttpHeaders.REFERER);
		if (originUrl == null) {
			originUrl = request.getHeaders().getOrigin();
		}
		String originDomain = null;
		if (originUrl != null) {
			originDomain = WebUtils.getDomain(originUrl);
			String[] urlSegs = StringUtils.split(originUrl, GlobalConstants.PATH_SEPARATOR);
			if (urlSegs.length > 1)
				return urlSegs[1];
		}
		return StringUtils.defaultString(originDomain, request.getURI().getAuthority());
	}

	public static String getIpAddr(ServerHttpRequest request) {

		String ip = request.getHeaders().getFirst(IpUtils.HEADER_FROWARDED_FOR);
		if (StringUtils.isBlank(ip) || IpUtils.UNKNOWN.equalsIgnoreCase(ip)) {
			ip = request.getHeaders().getFirst("Proxy-Client-IP");
		}
		if (StringUtils.isBlank(ip) || IpUtils.UNKNOWN.equalsIgnoreCase(ip)) {
			ip = request.getHeaders().getFirst("WL-Proxy-Client-IP");
		}
		/**
		 * 对于通过多个代理的情况， 第一个IP为客户端真实IP,多个IP按照','分割 x-forwarded-for=192.168.2.22,
		 * 192.168.1.92
		 */
		if (ip != null && ip.length() > 15) {
			String[] ips = StringUtils.split(ip, GlobalConstants.COMMA);
			for (String _ip : ips) {
				ip = StringUtils.trimToNull(_ip);
				if (!IpUtils.UNKNOWN.equalsIgnoreCase(ip)) {
					return ip;
				}
			}
		}
		//0:0:0:0:0:0:0:1
		if (ip != null && ip.contains(GlobalConstants.COLON)) {
			ip = IpUtils.LOCAL_BACK_IP;
		}else if(ip == null) {
			ip = request.getRemoteAddress().getAddress().getHostAddress();
		}
		
		return ip;
	}

	public static boolean isWebSocketRequest(ServerHttpRequest request) {
		return request.getHeaders().containsKey(WEBSOCKET_KEYS);
	}

	public static String resolveRouteName(String uri) {
		String contextPath = GatewayConstants.PATH_PREFIX;
		int indexOf = StringUtils.indexOf(uri, GlobalConstants.PATH_SEPARATOR, contextPath.length());
		uri = uri.substring(indexOf + 1);

		List<String> routeNames = CurrentSystemHolder.getRouteNames();
		for (String routeName : routeNames) {
			if (uri.startsWith(routeName + "/")) {
				return routeName;
			}
		}
		return GlobalRuntimeContext.APPID;
	}
	
	public static BizSystemModule getCurrentModule(ServerWebExchange exchange) {
		BizSystemModule module = exchange.getAttribute(GatewayConstants.CONTEXT_ROUTE_SERVICE);
		if(module != null)return module;
		String routeName = resolveRouteName(exchange.getRequest().getPath().value());
		module = CurrentSystemHolder.getModule(routeName);
		if(module != null) {
			exchange.getAttributes().put(GatewayConstants.CONTEXT_ROUTE_SERVICE, module);
		}
		return module;
	}
	
	public static ApiInfo getCurrentApi(ServerWebExchange exchange) {
		ApiInfo api = exchange.getAttribute(GatewayConstants.CONTEXT_CURRENT_API);
		if(api != null)return api;
		BizSystemModule module = getCurrentModule(exchange);
		ServerHttpRequest request = exchange.getRequest();
		ApiInfo apiInfo = module.getApiInfo(request.getMethodValue(), request.getPath().value());
		if(apiInfo != null) {
			exchange.getAttributes().put(GatewayConstants.CONTEXT_CURRENT_API, apiInfo);
		}
		return apiInfo;
	}
	
	public static String getCachingBodyString(ServerWebExchange exchange) {
    	if(exchange.getRequest().getMethod() == HttpMethod.GET) {
    		return null;
    	}
    	String bodyString = exchange.getAttribute(GatewayConstants.CACHED_REQUEST_BODY_STR_ATTR);
    	if(bodyString != null)return bodyString;
		DataBuffer dataBuffer = exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
		if(dataBuffer == null)return null;
		CharBuffer charBuffer = StandardCharsets.UTF_8.decode(dataBuffer.asByteBuffer());
        bodyString = charBuffer.toString();
        //
        exchange.getAttributes().put(GatewayConstants.CACHED_REQUEST_BODY_STR_ATTR, bodyString);
		return bodyString;
	}
	
	public static String getRequestHitKey(ServerWebExchange exchange) {
		String hitKey = exchange.getAttribute(GatewayConstants.CONTEXT_REQUEST_HIT_KEY);
		if(hitKey != null)return hitKey;
		
		ServerHttpRequest request = exchange.getRequest();
		
		StringBuilder builder = new StringBuilder();
		builder.append(request.getMethodValue()).append(request.getPath().value());
		
		Map<String, Object> paramMap = new HashMap<>();
		request.getQueryParams().forEach( (k,v) -> {
			if(!v.isEmpty())paramMap.put(k, v.get(0));
		} );
		if(request.getMethod() != HttpMethod.GET) {
			String cachingBody = getCachingBodyString(exchange);
			if(StringUtils.isNotBlank(cachingBody) && cachingBody.length() > 2) {
				paramMap.putAll(JsonUtils.toHashMap(cachingBody,Object.class));
			}
		}
		if(!paramMap.isEmpty()) {
			builder.append(ParameterUtils.mapToQueryParams(paramMap));
		}
		
		hitKey = builder.length() <= 64 ? builder.toString() : DigestUtils.md5(builder.toString());
		exchange.getAttributes().put(GatewayConstants.CONTEXT_REQUEST_HIT_KEY, hitKey);
		return hitKey;
	}
	
}
