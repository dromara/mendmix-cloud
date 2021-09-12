package com.jeesuite.springweb.client;

import java.util.Map;

import com.jeesuite.common.WebConstants;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.common.util.WebUtils;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.springweb.CurrentRuntimeContext;

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
		if(!headers.containsKey(WebConstants.HEADER_REQUEST_ID)){			 
			headers.put(WebConstants.HEADER_REQUEST_ID, TokenGenerator.generate());
		}
		if(!headers.containsKey(WebConstants.HEADER_AUTH_TOKEN)){			 
			headers.put(WebConstants.HEADER_AUTH_TOKEN, TokenGenerator.generateWithSign());
		}
		// 登录用户
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
		if(currentUser != null) {
			headers.put(WebConstants.HEADER_AUTH_USER, currentUser.toEncodeString());
		}
        //租户
		String tenantId = CurrentRuntimeContext.getTenantId(false);
		if (tenantId != null) {
			headers.put(WebConstants.HEADER_TENANT_ID, tenantId);
		}
		//标记不需要封装
		headers.put(WebConstants.HEADER_RESP_KEEP, Boolean.TRUE.toString());
		
		return headers;
	}
}
