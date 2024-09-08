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
package org.dromara.mendmix.gateway.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.util.TokenGenerator;
import org.dromara.mendmix.gateway.GatewayConfigs;
import org.dromara.mendmix.gateway.GatewayConstants;
import org.dromara.mendmix.gateway.helper.RequestContextHelper;

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
		if(!RequestContextHelper.getCurrentModule(exchange).isGateway()) {
			pass = GatewayConfigs.openApiEnabled && request.getHeaders().containsKey(CustomRequestHeaders.HEADER_OPEN_SIGN);
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
			String clientIp = RequestContextHelper.getIpAddr(request);
			if(GatewayConfigs.anonymousIpWhilelist.contains(clientIp))return true;
		}
		
		return false;
	}
	
	private boolean isInternalTrustedAccess(ServerHttpRequest request) {
		String header = request.getHeaders().getFirst(CustomRequestHeaders.HEADER_INTERNAL_REQUEST);
		if(Boolean.parseBoolean(header)) {
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
		} catch (Exception e) {
			System.err.println("validate [x-invoke-token = "+token+"] error" + e.getMessage());
		}
		
		return false;
	}

}
