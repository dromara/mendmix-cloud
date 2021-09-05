package com.jeesuite.mongo;

import com.jeesuite.common.ThreadLocalContext;

public class TenantHolder {

	public static String getTenantId() {
		return ThreadLocalContext.getStringValue(ThreadLocalContext.TENANT_ID_KEY);
	}
}
