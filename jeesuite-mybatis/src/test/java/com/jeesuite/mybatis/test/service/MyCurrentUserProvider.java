package com.jeesuite.mybatis.test.service;

import com.jeesuite.mybatis.plugin.auditfield.CurrentUserProvider;

public class MyCurrentUserProvider implements CurrentUserProvider {

	@Override
	public String currentUser() {
		return "admin555";
	}

}
