/*
 * Copyright 2016-2018 www.jeesuite.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeesuite.security;

import java.io.Serializable;
import java.util.List;

import com.jeesuite.security.Constants.CacheType;
import com.jeesuite.security.exception.UserNotFoundException;
import com.jeesuite.security.exception.UserPasswordWrongException;
import com.jeesuite.security.model.BaseUserInfo;
import com.jeesuite.security.model.UserSession;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月30日
 */
public abstract class SecurityDecisionProvider {

	public String sessionIdName(){
		return "JSESSION_ID";
	}
	
	public int sessionExpireIn(){
		return 7200;
	}
	
	public String cookieDomain(){
		return null;
	}
	
	public boolean multiPointEnable(){
		return true;
	}
	
	public boolean keepCookie(){
		return true;
	}
	
	public CacheType cacheType(){
		return CacheType.local;
	}
	
	public abstract String superAdminName();
	public abstract String contextPath();
	public abstract String[] anonymousUris();
	public abstract BaseUserInfo validateUser(String name,String password) throws UserNotFoundException,UserPasswordWrongException;
	public abstract List<String> findAllUriPermissionCodes();
	public abstract List<String> getUserPermissionCodes(Serializable userId);
	public abstract void authorizedPostHandle(UserSession session);
}
