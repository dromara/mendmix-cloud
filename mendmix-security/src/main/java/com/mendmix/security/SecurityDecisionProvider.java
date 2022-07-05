/*
 * Copyright 2016-2018 www.mendmix.com.
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
package com.mendmix.security;

import java.util.ArrayList;
import java.util.List;

import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.model.ApiModel;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.security.SecurityConstants.CacheType;
import com.mendmix.security.model.ApiPermission;

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
		return "Authorization";
	}
	
	public int sessionExpireIn(){
		return 1800;
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
	
	public boolean isServletType() {
		return true;
	}
	
	public CacheType cacheType(){
		if(CacheType.redis.name().equals(ResourceUtils.getProperty(SecurityConstants.CONFIG_STORAGE_TYPE))){
			return CacheType.redis;
		}
		return CacheType.local;
	}

	public List<ApiModel> anonymousUris() {
		return new ArrayList<>(0);
	}
	
	public abstract AuthUser validateUser(String type,String name,String password) throws MendmixBaseException;
	public abstract List<ApiPermission> getAllApiPermissions();
	public abstract List<ApiPermission> getUserApiPermissions(String userId);
	public abstract String error401Page();
	public abstract String error403Page();
}
