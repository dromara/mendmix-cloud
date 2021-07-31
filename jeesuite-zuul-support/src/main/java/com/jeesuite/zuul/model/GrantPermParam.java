package com.jeesuite.zuul.model;

import java.util.List;

public class GrantPermParam {

	private Integer roleId;
	private List<PermissionItem> items;
	
	public Integer getRoleId() {
		return roleId;
	}
	public void setRoleId(Integer roleId) {
		this.roleId = roleId;
	}
	public List<PermissionItem> getItems() {
		return items;
	}
	public void setItems(List<PermissionItem> items) {
		this.items = items;
	}
	
	
}
