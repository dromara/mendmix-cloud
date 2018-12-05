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
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.util.PathMatcher;

/**
 * 资源管理器
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年12月3日
 */
public class SecurityResourceManager {

	private static final String USER_PERMS_PRIFIX = "perms:";
	private static final String WILDCARD_START = "{";

	private Cache cache;
	private String contextPath;
	private SecurityDecisionProvider decisionProvider;

	private PathMatcher anonymousUriMatcher;
	// 无通配符uri
	//TODO 需要考虑多节点同步问题
	private volatile Map<String, String> nonWildcardUriPerms = new HashMap<>();
	private volatile Map<Pattern, String> wildcardUriPermPatterns = new HashMap<>();
	private volatile Map<String, String> uriPrefixs = new HashMap<>();

	public SecurityResourceManager(SecurityDecisionProvider decisionProvider, Cache cache) {
		this.decisionProvider = decisionProvider;
		this.cache = cache;

		contextPath = decisionProvider.contextPath();
		if (contextPath.endsWith("/")) {
			contextPath = contextPath.substring(0, contextPath.indexOf("/"));
		}
		anonymousUriMatcher = new PathMatcher(contextPath, decisionProvider.anonymousUris());

		loadPermissionCodes();
	}

	private void loadPermissionCodes() {
		List<String> permissionCodes = decisionProvider.findAllUriPermissionCodes();
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
	}

	public List<String> getUserPermissionCodes(Serializable userId) {
		
		String cacheKey = USER_PERMS_PRIFIX + userId;
		List<String> permissionCodes = cache.getObject(cacheKey);
		if(permissionCodes != null)return permissionCodes;
		
		permissionCodes = decisionProvider.getUserPermissionCodes(userId);
		List<String> removeWildcards = new ArrayList<>();
		for (String perm : permissionCodes) {
			if (perm.endsWith("*")) {
				removeWildcards.add(StringUtils.remove(perm, "*"));
			}
		}
		if (!removeWildcards.isEmpty())
			permissionCodes.addAll(removeWildcards);
		cache.setObject(cacheKey, permissionCodes);

		return permissionCodes;
	}

	public String getPermssionCode(String uri) {
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
		return anonymousUriMatcher.match(uri);
	}
	
	public void refreshUserPermssion(Serializable userId){
		String cacheKey = USER_PERMS_PRIFIX + userId;
		cache.remove(cacheKey);
	}

}
