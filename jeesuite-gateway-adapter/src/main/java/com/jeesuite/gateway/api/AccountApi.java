package com.jeesuite.gateway.api;

import java.util.List;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.gateway.model.AccountScope;

public interface AccountApi {

	AuthUser validateAccount(String type,String account,String password) throws JeesuiteBaseException;
	
	List<AccountScope> findAccountScopes(String accountId);
}
