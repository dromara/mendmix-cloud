package com.jeesuite.springweb.client;

import java.util.Map;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.spring.InstanceFactory;

public class RequestHeaderBuilder {

	private static RequestHeaderProvider headerProvider;
	
	static {
		headerProvider = InstanceFactory.getInstance(RequestHeaderProvider.class);
	}
	
	public static Map<String, String> getHeaders(){
		Map<String, String> headers = CurrentRuntimeContext.getCustomHeaders();
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
		headers.put(CustomRequestHeaders.HEADER_INVOKER_APP_ID, GlobalRuntimeContext.APPID);
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
		
		return headers;
	}
}
