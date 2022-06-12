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
package com.mendmix.gateway.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import com.mendmix.common.CustomRequestHeaders;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.util.TokenGenerator;
import com.mendmix.gateway.GatewayConfigs;
import com.mendmix.gateway.GatewayConstants;
import com.mendmix.gateway.helper.RuequestHelper;

/**
 * 
 * <br>
 * Class Name   : GatewayReactiveCustomAuthnHandler
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date May 14, 2022
 */
public class SpecUnauthorizedHandler {


	public boolean customAuthentication(ServerWebExchange exchange) {
		
		ServerHttpRequest request = exchange.getRequest();
		
		boolean pass = false;
		if(!RuequestHelper.getCurrentModule(exchange).isGateway()) {
			pass = GatewayConfigs.openApiEnabled && request.getHeaders().containsKey(GatewayConstants.X_SIGN_HEADER);
		}
		if(!pass) {
			pass = isIpWhilelistAccess(request);
		}
		if(!pass) {
			pass = isInternalTrustedAccess(request);
		}
		if(!pass) {
			pass = isCrossClusterTrustedAccess(request);
		}
		return pass;
	}

	/**
	 * 匿名访问白名单
	 * @param request
	 * @return
	 */
	private boolean isIpWhilelistAccess(ServerHttpRequest request) {

		if(!GatewayConfigs.anonymousIpWhilelist.isEmpty()) {
			String clientIp = RuequestHelper.getIpAddr(request);
			if(GatewayConfigs.anonymousIpWhilelist.contains(clientIp))return true;
		}
		
		return false;
	}
	
	private boolean isInternalTrustedAccess(ServerHttpRequest request) {
		String header = request.getHeaders().getFirst(CustomRequestHeaders.HEADER_IGNORE_AUTH);
		String header1 = request.getHeaders().getFirst(CustomRequestHeaders.HEADER_INTERNAL_REQUEST);
		if(Boolean.parseBoolean(header) && Boolean.parseBoolean(header1)) {
			if(validateInvokeToken(request)) {
				ThreadLocalContext.set(GatewayConstants.CONTEXT_TRUSTED_REQUEST, Boolean.TRUE);
				return true;
			}
		}
		return false;
	}
	
	private boolean isCrossClusterTrustedAccess(ServerHttpRequest request) {
		boolean crossCluster = false;
		try {
			String clusterName = request.getHeaders().getFirst(CustomRequestHeaders.HEADER_CLUSTER_ID);
			if(StringUtils.isNotBlank(clusterName)) {
				if(validateInvokeToken(request)) {
					ThreadLocalContext.set(GatewayConstants.CONTEXT_TRUSTED_REQUEST, Boolean.TRUE);
					crossCluster = true;
				}
			}
		} catch (Exception e) {}
		
		return crossCluster;
	}
	
	private boolean validateInvokeToken(ServerHttpRequest request) {
		String token = request.getHeaders().getFirst(CustomRequestHeaders.HEADER_INVOKE_TOKEN);
		if(StringUtils.isBlank(token))return false;
		try {
			TokenGenerator.validate(token, true);
			return true;
		} catch (Exception e) {}
		
		return false;
	}

}
