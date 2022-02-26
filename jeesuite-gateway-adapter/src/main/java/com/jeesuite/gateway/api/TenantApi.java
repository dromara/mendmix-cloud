package com.jeesuite.gateway.api;

import java.util.List;

import com.jeesuite.gateway.model.Tenant;

public interface TenantApi {

	Tenant userDefaultTenant(String userId);
	List<Tenant> userTenantList(String userId);
}
