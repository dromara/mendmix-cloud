/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.springweb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.springweb.CurrentRuntimeContext;

/**
 * 
 * <br>
 * Class Name   : BaseQueryParam
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年7月9日
 */
public class BaseQueryParam {

	@JsonIgnore
	private String currentTenantId;
	@JsonIgnore
	private String currentUserId;
	@JsonIgnore
	private String columns;//查询列
	
	

	/**
	 * @return the currentTenantId
	 */
	public String getCurrentTenantId() {
		return currentTenantId;
	}
	/**
	 * @param currentTenantId the currentTenantId to set
	 */
	public void setCurrentTenantId(String currentTenantId) {
		this.currentTenantId = currentTenantId;
	}

	/**
	 * @return the currentUserId
	 */
	public String getCurrentUserId() {
		return currentUserId;
	}
	/**
	 * @param currentUserId the currentUserId to set
	 */
	public void setCurrentUserId(String currentUserId) {
		this.currentUserId = currentUserId;
	}
	
	public String getColumns() {
		return columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	public void initLoginContext(){
		this.currentTenantId = CurrentRuntimeContext.getTenantId(false);
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
		if(currentUser == null)return;
		this.currentUserId = currentUser.getId();
	}
	
}
