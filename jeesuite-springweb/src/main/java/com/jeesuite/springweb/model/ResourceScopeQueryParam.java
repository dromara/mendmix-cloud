package com.jeesuite.springweb.model;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.model.AuthUser;

public class ResourceScopeQueryParam {

	private String tenantId;
	private String clientType;
	private String userType;
	private String userId;
	private String departmentId;
	private String type;

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
	

	public String getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(String departmentId) {
		this.departmentId = departmentId;
	}


	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public static ResourceScopeQueryParam current() {
		ResourceScopeQueryParam param = new ResourceScopeQueryParam();
		param.tenantId = CurrentRuntimeContext.getTenantId(false);
		param.clientType = CurrentRuntimeContext.getClientType();
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
		if(currentUser != null) {
			param.userId = currentUser.getId();
			param.userType = currentUser.getType();
		}
		return param;
	}
	
}
