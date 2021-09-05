package com.jeesuite.mybatis.plugin.autofield;

public interface CurrentUserProvider {

	String currentUser();
	
	String currentTenant();
}
