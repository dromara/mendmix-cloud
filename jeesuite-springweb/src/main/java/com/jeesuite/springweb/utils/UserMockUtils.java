package com.jeesuite.springweb.utils;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.ResourceUtils;

public class UserMockUtils {

	private static boolean enabled = ResourceUtils.getBoolean("jeesuite.mock.context.enabled", false);
	
	public static boolean isEnabled() {
		return enabled && GlobalRuntimeContext.isDevEnv();
	}

	public static AuthUser initMockContextIfOnCondition() {
		AuthUser authUser = CurrentRuntimeContext.getCurrentUser();
		if(authUser == null && isEnabled()){
    		CurrentRuntimeContext.setClientType(ResourceUtils.getProperty("jeesuite.mock.context.clientType"));
			CurrentRuntimeContext.setTenantId(ResourceUtils.getProperty("jeesuite.mock.context.tenantId"));
    		CurrentRuntimeContext.setSystemId(ResourceUtils.getProperty("jeesuite.mock.context.systemId"));
    		
    		authUser = new AuthUser();
    		authUser.setId(ResourceUtils.getProperty("jeesuite.mock.context.user.id","1"));
    		authUser.setPrincipalType(ResourceUtils.getProperty("jeesuite.mock.context.user.principalType","staff"));
    		authUser.setPrincipalId(ResourceUtils.getProperty("jeesuite.mock.context.user.principalId","1"));
    		authUser.setName(ResourceUtils.getProperty("jeesuite.mock.context.user.name","mockuser"));
    		authUser.setType(ResourceUtils.getProperty("jeesuite.mock.context.user.type"));
    		authUser.setDeptId(ResourceUtils.getProperty("jeesuite.mock.context.user.deptId"));
    		authUser.setPostId(ResourceUtils.getProperty("jeesuite.mock.context.user.postId"));
    		authUser.setAdmin(ResourceUtils.getBoolean("jeesuite.mock.context.user.isAdmin"));
    		CurrentRuntimeContext.setAuthUser(authUser);
    	}
		return authUser;
	}
}
