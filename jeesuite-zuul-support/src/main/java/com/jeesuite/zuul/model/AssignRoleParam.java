package com.jeesuite.zuul.model;

import java.util.List;

/**
 * 
 * <br>
 * Class Name   : AssignRoleParam
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年12月31日
 */
public class AssignRoleParam  {

	private String userId;
	private String userName;
	private List<Integer> roleIds;
	
	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * @return the roleIds
	 */
	public List<Integer> getRoleIds() {
		return roleIds;
	}
	/**
	 * @param roleIds the roleIds to set
	 */
	public void setRoleIds(List<Integer> roleIds) {
		this.roleIds = roleIds;
	}


	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
