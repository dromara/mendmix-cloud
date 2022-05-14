package com.jeesuite.gateway.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.gateway.CurrentSystemHolder;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.security.SecurityDecisionProvider;
import com.jeesuite.security.model.ApiPermission;

public abstract class GatewaySecurityDecisionProvider extends SecurityDecisionProvider {
	
	@Override
	public List<ApiPermission> getAllApiPermissions() {
		List<BizSystemModule> modules = CurrentSystemHolder.getModules();
		
		List<ApiPermission> result = new ArrayList<>();
		Collection<ApiInfo> apis;
		ApiPermission apiPermission;
		for (BizSystemModule module : modules) {
			if(module.getApiInfos() == null)continue;
			apis = module.getApiInfos().values();
			for (ApiInfo apiInfo : apis) {
				apiPermission = new ApiPermission();
				apiPermission.setGrantType(apiInfo.getPermissionType().name());
				apiPermission.setHttpMethod(apiInfo.getMethod());
				apiPermission.setUri(apiInfo.getUrl());
				result.add(apiPermission);
			}
		}
		return result;
	}
}