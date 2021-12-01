package com.jeesuite.springweb.client;

import java.util.Map;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.common.util.WebUtils;
import com.jeesuite.spring.InstanceFactory;

public class RequestHeaderBuilder {

	private static RequestHeaderProvider headerProvider;
	
	static {
		headerProvider = InstanceFactory.getInstance(RequestHeaderProvider.class);
	}
	
	public static Map<String, String> getHeaders(){
		Map<String, String> headers = WebUtils.getCustomHeaders();
		//
		Map<String, String> appHeaders = null;
		if(headerProvider != null)appHeaders = headerProvider.headers();
		if(appHeaders != null && !appHeaders.isEmpty()) {
			headers.putAll(appHeaders);
		}
		if(!headers.containsKey(CustomRequestHeaders.HEADER_REQUEST_ID)){			 
			headers.put(CustomRequestHeaders.HEADER_REQUEST_ID, TokenGenerator.generate());
		}
		if(!headers.containsKey(CustomRequestHeaders.HEADER_AUTH_TOKEN)){			 
			headers.put(CustomRequestHeaders.HEADER_AUTH_TOKEN, TokenGenerator.generateWithSign());
		}
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
		
		return headers;
	}
}
