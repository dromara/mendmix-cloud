package com.jeesuite.zuul;

import java.util.List;

import com.jeesuite.common.model.IdNamePair;

public interface TenantApi {

	IdNamePair defaultTenant();
	List<IdNamePair> tenantList();
}
