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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.constants.PermissionLevel;
import com.jeesuite.common.util.PathMatcher;
import com.jeesuite.security.model.ApiPermission;
import com.jeesuite.security.model.UserSession;

/**
 * 资源管理器
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年12月3日
 */
public class SecurityResourceManager {

	private static Logger log = LoggerFactory.getLogger("com.jeesuite.security");

	public final static String WILDCARD_START = "{";

	private List<String> authzUris = new ArrayList<>();
	private List<Pattern> authzPatterns = new ArrayList<>();
	private List<String> anonUris = new ArrayList<>();
	private List<Pattern> anonUriPatterns = new ArrayList<>();
	// 所有无通配符uri
	private List<String> nonWildcardUris = new ArrayList<>();

	private SecurityDecisionProvider decisionProvider;

	private ScheduledExecutorService refreshExecutor = Executors.newScheduledThreadPool(1);

	private String cacheName = "permres";
	private SecurityStorageManager storageManager;
	
	private PathMatcher anonymousUrlMatcher;

	public SecurityResourceManager(SecurityDecisionProvider decisionProvider, SecurityStorageManager storageManager) {
		this.storageManager = storageManager;
		this.storageManager.addCahe(cacheName, decisionProvider.sessionExpireIn());
		storageManager.addCahe(cacheName, decisionProvider.sessionExpireIn());
		this.decisionProvider = decisionProvider;
		//
		if(decisionProvider.anonymousUrlPatterns() != null){
			anonymousUrlMatcher = new PathMatcher(GlobalRuntimeContext.getContextPath(), decisionProvider.anonymousUrlPatterns());
		}
		
		if(decisionProvider.apiAuthzEnabled()) {
			refreshExecutor.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					loadApiPermissions();
				}
			}, 10, 60, TimeUnit.SECONDS);
		}
	}

	public List<String> getAuthorizationUris() {
		return authzUris;
	}

	public List<String> getAuthzUris() {
		return authzUris;
	}

	public List<Pattern> getAuthzPatterns() {
		return authzPatterns;
	}

	public List<String> getAnonUris() {
		return anonUris;
	}

	public List<Pattern> getAnonUriPatterns() {
		return anonUriPatterns;
	}

	public List<String> getNonWildcardUris() {
		return nonWildcardUris;
	}

	
	public synchronized boolean loadApiPermissions() {

		log.info("============begin load perssion data==============");
		List<ApiPermission> permissions = decisionProvider.getAllApiPermissions();
		
		if(permissions == null)return false;

		boolean withWildcard;
		String permissionKey;
		Pattern pattern;
		for (ApiPermission permission : permissions) {
			withWildcard = permission.getUri().contains(WILDCARD_START);
			permissionKey = ApiPermssionCheckHelper.buildPermissionKey(permission.getHttpMethod(), permission.getUri());
			if (!withWildcard) {
				nonWildcardUris.add(permissionKey);
			}
			if (PermissionLevel.PermissionRequired.name().equals(permission.getGrantType())) {
				if (withWildcard) {
					pattern = Pattern.compile(permissionKey.replaceAll("\\{[^/]+?\\}", ".+"));
					authzPatterns.add(pattern);
				} else {
					authzUris.add(permissionKey);
				}
			} else if (PermissionLevel.Anonymous.name().equals(permission.getGrantType())) {
				if (withWildcard) {
					pattern = Pattern.compile(permissionKey.replaceAll("\\{[^/]+?\\}", ".+"));
					anonUriPatterns.add(pattern);
				} else {
					anonUris.add(permissionKey);
				}
			}
		}

		log.info("nonWildcardUris:         {}", nonWildcardUris);
		log.info("anonUris:                {}", anonUris);
		log.info("anonUriPatterns:         {}", anonUriPatterns);
		log.info("authzUris:               {}", authzUris);
		log.info("authzPatterns:           {}", authzPatterns);
		log.info("============load perssion data finish==============");

		return true;
	}
	
	public List<String> getUserPermissions(UserSession session) {

		// TODO 考虑统一用户在不同租户，不同平台,逻辑要补充
		String cacheKey = String.format("%s_%s", StringUtils.trimToEmpty(session.getTenantId()),
				session.getUser().getId());
		List<String> permissionCodes = storageManager.getCache(cacheName).getObject(cacheKey);
		if (permissionCodes != null)
			return permissionCodes;

		permissionCodes = decisionProvider.getUserApiPermissionUris(session.getUser().getId());

		if (permissionCodes == null) {
			permissionCodes = new ArrayList<>(0);
		} else if (!permissionCodes.isEmpty()) {
			permissionCodes = new ArrayList<>(permissionCodes);
			List<String> removeWildcards = new ArrayList<>();
			for (String perm : permissionCodes) {
				if (perm.endsWith("*")) {
					removeWildcards.add(StringUtils.remove(perm, "*"));
				}
			}
			if (!removeWildcards.isEmpty())
				permissionCodes.addAll(removeWildcards);
		}

		storageManager.getCache(cacheName).setObject(cacheKey, permissionCodes);

		return permissionCodes;
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

	public void close() {
		if (refreshExecutor != null)
			refreshExecutor.shutdown();
	}

}
