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

import java.util.List;

import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.security.SecurityConstants.CacheType;
import com.jeesuite.security.exception.UserNotFoundException;
import com.jeesuite.security.exception.UserPasswordWrongException;
import com.jeesuite.security.model.ApiPermission;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月30日
 */
public abstract class SecurityDecisionProvider {

	public String sessionIdName(){
		return "JSESSIONID";
	}
	
	public String headerTokenName(){
		return "x-user-token";
	}
	
	public int sessionExpireIn(){
		return 7200;
	}
	
	public String cookieDomain(){
		return null;
	}
	
	public boolean kickOff(){
		return true;
	}
	
	public boolean keepCookie(){
		return true;
	}
	
	public boolean oauth2Enabled(){
		return true;
	}
	
	public boolean apiAuthzEnabled(){
		return true;
	}
	
	public String superAdminName(){
		return "sa";
	}
	
	public CacheType cacheType(){
		if(CacheType.redis.name().equals(ResourceUtils.getProperty(SecurityConstants.CONFIG_STORAGE_TYPE))){
			return CacheType.redis;
		}
		return CacheType.local;
	}

	
	public abstract List<String> anonymousUrlPatterns();
	public abstract AuthUser validateUser(String name,String password) throws UserNotFoundException,UserPasswordWrongException;
	public abstract List<ApiPermission> getAllApiPermissions();
	public abstract List<String> getUserApiPermissionUris(String userId);
	public abstract String error401Page();
	public abstract String error403Page();
}
