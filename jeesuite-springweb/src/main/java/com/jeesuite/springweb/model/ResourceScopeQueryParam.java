package com.jeesuite.springweb.model;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.model.AuthUser;

public class ResourceScopeQueryParam {

	private String tenantId;
	private String clientType;
	private String userType;
	private String userId;
	private String resourceType;

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getClientType() {
		return clientType;
	}

	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

	public String getUserType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public static ResourceScopeQueryParam current() {
		ResourceScopeQueryParam param = new ResourceScopeQueryParam();
		param.tenantId = CurrentRuntimeContext.getTenantId(false);
		param.clientType = CurrentRuntimeContext.getClientType();
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
		if(currentUser != null) {
			param.userId = currentUser.getId();
			param.userType = currentUser.getUserType();
		}
		return param;
	}
	
}
