package com.jeesuite.mybatis.test.service;

import com.jeesuite.mybatis.plugin.autofield.CurrentUserProvider;

public class MyCurrentUserProvider implements CurrentUserProvider {

	@Override
	public String currentUser() {
		return "admin555";
	}

	@Override
	public String currentTenant() {
		return null;
	}

}
