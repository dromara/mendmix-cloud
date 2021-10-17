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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.util.PathMatcher;
import com.jeesuite.security.SecurityConstants.CacheType;
import com.jeesuite.security.model.UserSession;

/**
 * 资源管理器
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年12月3日
 */
public class SecurityResourceManager {

	private static final String WILDCARD_START = "{";

	private String contextPath;
	private SecurityDecisionProvider decisionProvider;

	private PathMatcher anonymousUrlMatcher;
	// 无通配符uri
	private volatile Map<String, String> nonWildcardUriPerms = new HashMap<>();
	private volatile Map<Pattern, String> wildcardUriPermPatterns = new HashMap<>();
	private volatile Map<String, String> uriPrefixs = new HashMap<>();
	
	private volatile boolean refreshCallable = true;
	private ScheduledExecutorService refreshExecutor = Executors.newScheduledThreadPool(1);
			
	private static String cacheName = "permres";
	private SecurityStorageManager storageManager;

	public SecurityResourceManager(SecurityDecisionProvider decisionProvider,SecurityStorageManager storageManager) {
		this.storageManager = storageManager;
		this.storageManager.addCahe(cacheName, decisionProvider.sessionExpireIn());
		storageManager.addCahe(cacheName, decisionProvider.sessionExpireIn());
		this.decisionProvider = decisionProvider;
		contextPath = decisionProvider.contextPath();
		if (contextPath.endsWith("/")) {
			contextPath = contextPath.substring(0, contextPath.indexOf("/"));
		}
		
		if(decisionProvider.anonymousUrlPatterns() != null){
			anonymousUrlMatcher = new PathMatcher(contextPath, decisionProvider.anonymousUrlPatterns());
		}
		//
		final boolean forceRefresh = CacheType.redis == decisionProvider.cacheType();
		refreshExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				loadPermissionCodes(forceRefresh);
			}
		}, 1, forceRefresh ? 30 : 5, TimeUnit.SECONDS);
	}

	private synchronized void loadPermissionCodes( boolean forceRefresh) {
		if(!forceRefresh && !refreshCallable)return;
		
		Map<String, String> nonWildcardUriPerms = new HashMap<>();
		Map<Pattern, String> wildcardUriPermPatterns = new HashMap<>();
		Map<String, String> uriPrefixs = new HashMap<>();
		List<String> permissionCodes = decisionProvider.getAllApiPermissions();
		if(permissionCodes == null || permissionCodes.isEmpty())return;
		String fullUri = null;
		for (String permCode : permissionCodes) {
			fullUri = contextPath + permCode;
			if (permCode.contains(WILDCARD_START)) {
				String regex = fullUri.replaceAll("\\{.*?(?=})", ".*").replaceAll("\\}", "");
				wildcardUriPermPatterns.put(Pattern.compile(regex), permCode);
			} else if (fullUri.endsWith("*")) {
				uriPrefixs.put(StringUtils.remove(fullUri, "*"), permCode);
			} else {
				nonWildcardUriPerms.put(fullUri, permCode);
			}
		}
		
		this.nonWildcardUriPerms = nonWildcardUriPerms;
		this.wildcardUriPermPatterns = wildcardUriPermPatterns;
		this.uriPrefixs = uriPrefixs;
		
		refreshCallable = false;
	}

	public List<String> getUserPermissionCodes(UserSession session) {
		
		//TODO 考虑统一用户在不同租户，不同平台,逻辑要补充
		String cacheKey = String.format("%s_%s",StringUtils.trimToEmpty(session.getTenantId()) ,session.getUser().getId());
		List<String> permissionCodes = storageManager.getCache(cacheName).getObject(cacheKey);
		if(permissionCodes != null)return permissionCodes;
		
		permissionCodes = decisionProvider.getUserApiPermissions(session.getUser().getId());
		
		if(permissionCodes == null) {
			permissionCodes = new ArrayList<>(0);
		}else if(!permissionCodes.isEmpty()) {
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

	public String getPermssionCode(String uri) {
		if(isAnonymous(uri))return null;
		if (nonWildcardUriPerms.containsKey(uri))
			return nonWildcardUriPerms.get(uri);

		for (Pattern pattern : wildcardUriPermPatterns.keySet()) {
			if (pattern.matcher(uri).matches())
				return wildcardUriPermPatterns.get(pattern);
		}

		for (String prefix : uriPrefixs.keySet()) {
			if (uri.startsWith(prefix)) {
				return uriPrefixs.get(prefix);
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
	
	public void refreshUserPermssions(Serializable userId){
		storageManager.getCache(cacheName).remove(String.valueOf(userId));
	}
	
	public void refreshUserPermssions(){
		storageManager.getCache(cacheName).removeAll();
	}
	
	public void refreshResources(){
		refreshCallable = true;
	}
	
	public void close(){
		if(refreshExecutor != null)refreshExecutor.shutdown();
	}

}
