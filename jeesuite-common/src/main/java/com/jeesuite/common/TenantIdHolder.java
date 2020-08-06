package com.jeesuite.common;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */

/**
 * 
 * <br>
 * Class Name   : TenantIdHolder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年8月1日
 */
public class TenantIdHolder {

	private static final List<String> tenantIds = new ArrayList<>();
	
	public static void addTenantId(String tenantId){
		if(tenantIds.contains(tenantId))return;
		tenantIds.add(tenantId);
	}

	public static List<String> getTenantids() {
		return Collections.unmodifiableList(tenantIds);
	}
}
