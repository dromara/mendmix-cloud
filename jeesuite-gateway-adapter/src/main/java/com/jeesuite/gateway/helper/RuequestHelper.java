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
package com.jeesuite.gateway.helper;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import com.jeesuite.common.GlobalConstants;
import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.util.IpUtils;
import com.jeesuite.common.util.WebUtils;
import com.jeesuite.gateway.CurrentSystemHolder;
import com.jeesuite.gateway.GatewayConstants;

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

		if (request.getRemoteAddress().getAddress().isLoopbackAddress()) {
			return IpUtils.LOCAL_BACK_IP;
		}
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
			String[] ips = StringUtils.split(ip, ",");
			for (String _ip : ips) {
				ip = StringUtils.trimToNull(_ip);
				if (!IpUtils.UNKNOWN.equalsIgnoreCase(ip)) {
					return ip;
				}
			}
		}
		return ip;
	}

	public static boolean isWebSocketRequest(ServerHttpRequest request) {
		return request.getHeaders().containsKey(WEBSOCKET_KEYS);
	}

	public static String getCurrentRouteName(ServerHttpRequest request) {
		String contextPath = GatewayConstants.PATH_PREFIX;
		String uri = request.getPath().value();
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
}
