package com.jeesuite.common.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserContextQueryParam {

	@JsonIgnore
	private String currentTenantId;
	
	@JsonIgnore
	private String currentUserId;
	
	@JsonIgnore
	private String currentUnitId;
	
	@JsonIgnore
	Map<String, String[]> dataProfileValues;

	public String getCurrentTenantId() {
		return currentTenantId;
	}

	public void setCurrentTenantId(String currentTenantId) {
		this.currentTenantId = currentTenantId;
	}

	public String getCurrentUserId() {
		return currentUserId;
	}

	public void setCurrentUserId(String currentUserId) {
		this.currentUserId = currentUserId;
	}

	public String getCurrentUnitId() {
		return currentUnitId;
	}

	public void setCurrentUnitId(String currentUnitId) {
		this.currentUnitId = currentUnitId;
	}

	public Map<String, String[]> getDataProfileValues() {
		return dataProfileValues;
	}

	public void setDataProfileValues(Map<String, String[]> dataProfileValues) {
		this.dataProfileValues = dataProfileValues;
	}
	
	
}
