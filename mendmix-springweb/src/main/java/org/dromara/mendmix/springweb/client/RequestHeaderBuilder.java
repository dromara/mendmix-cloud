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
package org.dromara.mendmix.springweb.client;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.util.TokenGenerator;
import org.dromara.mendmix.spring.InstanceFactory;

public class RequestHeaderBuilder {
	
	public static List<String> sensitiveHeaders = Arrays.asList(
			CustomRequestHeaders.HEADER_TENANT_ID,
			CustomRequestHeaders.HEADER_AUTH_USER,
			CustomRequestHeaders.HEADER_IGNORE_TENANT,
			CustomRequestHeaders.HEADER_IGNORE_AUTH,
			CustomRequestHeaders.HEADER_CLUSTER_ID
		 );

	private static RequestHeaderProvider headerProvider;
	
	static {
		headerProvider = InstanceFactory.getInstance(RequestHeaderProvider.class);
	}
	
	public static Map<String, String> getHeaders(){
		Map<String, String> headers = CurrentRuntimeContext.getContextHeaders();
		//
		Map<String, String> appHeaders = null;
		if(headerProvider != null)appHeaders = headerProvider.headers();
		if(appHeaders != null && !appHeaders.isEmpty()) {
			headers.putAll(appHeaders);
		}
		if(!headers.containsKey(CustomRequestHeaders.HEADER_REQUEST_ID)){			 
			headers.put(CustomRequestHeaders.HEADER_REQUEST_ID, TokenGenerator.generate());
		}
		if(!headers.containsKey(CustomRequestHeaders.HEADER_INVOKE_TOKEN)){			 
			headers.put(CustomRequestHeaders.HEADER_INVOKE_TOKEN, TokenGenerator.generateWithSign());
		}
		headers.put(CustomRequestHeaders.HEADER_INVOKER_APP_ID, GlobalContext.APPID);
		// 登录用户
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
		if(currentUser != null) {
			headers.put(CustomRequestHeaders.HEADER_AUTH_USER, currentUser.toEncodeString());
		}
        //租户
		String tenantId = CurrentRuntimeContext.getTenantId(false);
		if (tenantId != null) {
			headers.put(CustomRequestHeaders.HEADER_TENANT_ID, tenantId);
		}
		
		String clientType = CurrentRuntimeContext.getClientType();
		if (clientType != null) {
			headers.put(CustomRequestHeaders.HEADER_CLIENT_TYPE, clientType);
		}
		
		String systemId = CurrentRuntimeContext.getSystemId();
		if (systemId != null) {
			headers.put(CustomRequestHeaders.HEADER_SYSTEM_ID, systemId);
		}
	
		return headers;
	}
}
