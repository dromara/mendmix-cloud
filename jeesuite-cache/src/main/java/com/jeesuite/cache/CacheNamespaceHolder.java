/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.cache;

import com.jeesuite.common.ThreadLocalContext;
import com.jeesuite.common.util.ResourceUtils;

/**
 * 
 * <br>
 * Class Name   : CacheNamespaceHolder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年6月9日
 */
public class CacheNamespaceHolder {

	private static final String TENANT_ID_KEY = "_ctx_tenantId_";
	public final static boolean tenantModeEnabled = ResourceUtils.getBoolean("jeesuite.cache.tenantModeEnabled", false);
	
	public static String getTenantIdKeyPrefix(){
		if(!tenantModeEnabled)return null;
		return ThreadLocalContext.getStringValue(TENANT_ID_KEY);
	}
	
	public static void setTenantId(String tenantId){
		ThreadLocalContext.set(TENANT_ID_KEY, tenantId);
	}

}
