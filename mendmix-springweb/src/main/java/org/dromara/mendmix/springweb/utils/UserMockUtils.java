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
package org.dromara.mendmix.springweb.utils;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.util.ResourceUtils;

public class UserMockUtils {

	private static boolean enabled = ResourceUtils.getBoolean("mendmix-cloud.mock.context.enabled", false);
	
	public static boolean isEnabled() {
		return enabled && GlobalContext.isDevEnv();
	}

	public static AuthUser initMockContextIfOnCondition() {
		AuthUser authUser = CurrentRuntimeContext.getCurrentUser();
		if(authUser == null && isEnabled()){
    		CurrentRuntimeContext.setClientType(ResourceUtils.getProperty("mendmix-cloud.mock.context.clientType"));
			CurrentRuntimeContext.setTenantId(ResourceUtils.getProperty("mendmix-cloud.mock.context.tenantId"));
    		CurrentRuntimeContext.setSystemId(ResourceUtils.getProperty("mendmix-cloud.mock.context.systemId"));
    		//
    		authUser = ResourceUtils.getBean("mendmix-cloud.mock.context.user.", AuthUser.class);
    		CurrentRuntimeContext.setAuthUser(authUser);
    	}
		return authUser;
	}
}
