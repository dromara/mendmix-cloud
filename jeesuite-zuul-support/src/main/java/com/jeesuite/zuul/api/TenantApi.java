package com.jeesuite.zuul.api;

import java.util.List;

import com.jeesuite.zuul.model.Tenant;

public interface TenantApi {

	Tenant userDefaultTenant(String userId);
	List<Tenant> userTenantList(String userId);
}
