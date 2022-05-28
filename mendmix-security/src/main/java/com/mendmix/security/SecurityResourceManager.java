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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.constants.PermissionLevel;
import com.mendmix.common.util.PathMatcher;
import com.mendmix.security.model.ApiPermission;
import com.mendmix.security.model.UserSession;

/**
 * 资源管理器
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年12月3日
 */
public class SecurityResourceManager {

	private static Logger log = LoggerFactory.getLogger("com.mendmix.security");

	private AtomicReference<List<String>> authzUris = new AtomicReference<>();
	private AtomicReference<List<Pattern>> authzPatterns = new AtomicReference<>();
	private AtomicReference<List<String>> anonUris = new AtomicReference<>();
	private AtomicReference<List<Pattern>> anonUriPatterns = new AtomicReference<>();
	// 所有无通配符uri
	private AtomicReference<List<String>> nonWildcardUris = new AtomicReference<>();

	private SecurityDecisionProvider decisionProvider;

	private String cacheName = "permres";
	private SecurityStorageManager storageManager;
	
	private PathMatcher anonymousUrlMatcher;
	
	private boolean logging = true;

	public SecurityResourceManager(SecurityDecisionProvider decisionProvider, SecurityStorageManager storageManager) {
		this.storageManager = storageManager;
		this.storageManager.addCahe(cacheName, decisionProvider.sessionExpireIn());
		storageManager.addCahe(cacheName, decisionProvider.sessionExpireIn());
		this.decisionProvider = decisionProvider;
		//
		if(decisionProvider.anonymousUrlPatterns() != null){
			anonymousUrlMatcher = new PathMatcher(GlobalRuntimeContext.getContextPath(), decisionProvider.anonymousUrlPatterns());
		}
		//
		loadApiPermissions();
	}


	public List<String> getAuthzUris() {
		return authzUris.get() == null ? new ArrayList<>(0) : authzUris.get();
	}

	public List<Pattern> getAuthzPatterns() {
		return authzPatterns.get() == null ? new ArrayList<>(0) : authzPatterns.get();
	}

	public List<String> getAnonUris() {
		return anonUris.get() == null ? new ArrayList<>(0) : anonUris.get();
	}

	public List<Pattern> getAnonUriPatterns() {
		return anonUriPatterns.get() == null ? new ArrayList<>(0) : anonUriPatterns.get();
	}

	public List<String> getNonWildcardUris() {
		return nonWildcardUris.get() == null ? new ArrayList<>(0) : nonWildcardUris.get();
	}

	
	public synchronized boolean loadApiPermissions() {

		List<ApiPermission> permissions = decisionProvider.getAllApiPermissions();
		
		if(permissions == null)return false;
		
		List<String> _authzUris = new ArrayList<>();
		List<Pattern> _authzPatterns = new ArrayList<>();
		List<String> _anonUris = new ArrayList<>();
		List<Pattern> _anonUriPatterns = new ArrayList<>();
		// 所有无通配符uri
		List<String> _nonWildcardUris = new ArrayList<>();

		boolean withWildcard;
		String permissionKey;
		Pattern pattern;
		for (ApiPermission permission : permissions) {
			withWildcard = permission.getUri().contains(ApiPermssionCheckHelper.WILDCARD_START);
			permissionKey = ApiPermssionCheckHelper.buildPermissionKey(permission.getHttpMethod(), permission.getUri());
			if (!withWildcard) {
				_nonWildcardUris.add(permissionKey);
			}
			if (PermissionLevel.PermissionRequired.name().equals(permission.getGrantType())) {
				if (withWildcard) {
					pattern = Pattern.compile(ApiPermssionCheckHelper.pathVariableToPattern(permissionKey));
					_authzPatterns.add(pattern);
				} else {
					_authzUris.add(permissionKey);
				}
			} else if (PermissionLevel.Anonymous.name().equals(permission.getGrantType())) {
				if (withWildcard) {
					pattern = Pattern.compile(ApiPermssionCheckHelper.pathVariableToPattern(permissionKey));
					_anonUriPatterns.add(pattern);
				} else {
					_anonUris.add(permissionKey);
				}
			}
		}
		
		nonWildcardUris.set(_nonWildcardUris);
		anonUris.set(_anonUris);
		anonUriPatterns.set(_anonUriPatterns);
		authzUris.set(_authzUris);
		authzPatterns.set(_authzPatterns);
		
        if(logging) {
        	log.info("nonWildcardUris:         {}", getNonWildcardUris());
    		log.info("anonUris:                {}", getAnonUris());
    		log.info("anonUriPatterns:         {}", getAnonUriPatterns());
    		log.info("authzUris:               {}", getAuthzUris());
    		log.info("authzPatterns:           {}", getAuthzPatterns());
    		logging = false;
        }

		return true;
	}
	
	public List<String> getUserPermissions(UserSession session) {

		String hashKey = "apis";
		if(StringUtils.isNotBlank(session.getTenantId())) {
			hashKey = "apis_" + session.getTenantId();
		}
		
		List<String> permissionCodes = storageManager.getCache(cacheName).getMapValue(session.getSessionId(), hashKey);
		if (permissionCodes != null) {
			return permissionCodes;
		}

		List<ApiPermission> apiPermissions = decisionProvider.getUserApiPermissions(session.getUser().getId());
        
		if (apiPermissions == null) {
			permissionCodes = new ArrayList<>(0);
		} else if (!apiPermissions.isEmpty()) {
			permissionCodes = new ArrayList<>(apiPermissions.size());
			String permissionCode;
			for(ApiPermission api : apiPermissions) {
				permissionCode = resovlePermissionKey(api);
				if(permissionCode == null)continue;
				permissionCodes.add(permissionCode);
	        }
		}
		storageManager.getCache(cacheName).setMapValue(session.getSessionId(), hashKey, permissionCodes);

		return permissionCodes;
	}
	
	private String resovlePermissionKey(ApiPermission api) {
		if(api.getUri().contains(ApiPermssionCheckHelper.WILDCARD_START)) {
			String uriPattern = ApiPermssionCheckHelper.pathVariableToPattern(api.getUri());
			List<Pattern> patterns = authzPatterns.get();
			for (Pattern pattern : patterns) {		
				if(pattern.pattern().startsWith(api.getHttpMethod()) && pattern.pattern().endsWith(uriPattern)) {
					return pattern.pattern();
				}
			}
		}else {
			for (String authzUri : authzUris.get()) {
				if(authzUri.startsWith(api.getHttpMethod()) && authzUri.endsWith(api.getUri())) {
					return authzUri;
				}
			}
		}
		return null;
	}

	public boolean isAnonymous(String uri){
	       if(anonymousUrlMatcher != null){
				return anonymousUrlMatcher.match(uri);
			}
			return false;
		}


	public void refreshUserPermssions(Serializable userId) {
		storageManager.getCache(cacheName).remove(String.valueOf(userId));
	}

	public void refreshUserPermssions() {
		storageManager.getCache(cacheName).removeAll();
	}

	public void close() {}

}
