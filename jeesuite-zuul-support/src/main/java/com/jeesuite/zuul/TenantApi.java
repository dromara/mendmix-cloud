package com.jeesuite.zuul;

import java.util.List;

import com.jeesuite.zuul.model.Tenant;

public interface TenantApi {

	Tenant defaultTenant();
	List<Tenant> tenantList();
}
