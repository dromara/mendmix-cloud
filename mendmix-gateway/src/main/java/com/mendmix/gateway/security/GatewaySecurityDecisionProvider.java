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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mendmix.common.async.AsyncInitializer;
import com.mendmix.common.model.ApiInfo;
import com.mendmix.gateway.CurrentSystemHolder;
import com.mendmix.gateway.GatewayConstants;
import com.mendmix.gateway.model.BizSystemModule;
import com.mendmix.security.SecurityDecisionProvider;
import com.mendmix.security.SecurityDelegating;
import com.mendmix.security.model.ApiPermission;

public abstract class GatewaySecurityDecisionProvider extends SecurityDecisionProvider implements AsyncInitializer {
	
	
	@Override
	public boolean isServletType() {
		return false;
	}
	
	@Override
	public String resolveUri(String uri) {
		return GatewayConstants.PATH_PREFIX + uri;
	}
	

	@Override
	public List<ApiPermission> getAllApiPermissions() {
		Collection<BizSystemModule> modules = CurrentSystemHolder.getModules();
		
		List<ApiPermission> result = new ArrayList<>();
		Collection<ApiInfo> apis;
		ApiPermission apiPermission;
		for (BizSystemModule module : modules) {
			if(module.getApiInfos() == null)continue;
			apis = module.getApiInfos().values();
			for (ApiInfo apiInfo : apis) {
				apiPermission = new ApiPermission();
				apiPermission.setPermissionLevel(apiInfo.getPermissionLevel());
				apiPermission.setMethod(apiInfo.getMethod());
				apiPermission.setUri(apiInfo.getUri());
				result.add(apiPermission);
			}
		}
		return result;
	}

	@Override
	public void doInitialize() {
		SecurityDelegating.init();
	}

	
}
