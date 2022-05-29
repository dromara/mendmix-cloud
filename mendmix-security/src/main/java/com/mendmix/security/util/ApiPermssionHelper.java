/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.security.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.GlobalConstants;
import com.mendmix.common.constants.PermissionLevel;
import com.mendmix.common.model.ApiModel;
import com.mendmix.security.SecurityDecisionProvider;
import com.mendmix.security.SecurityDelegating;
import com.mendmix.security.model.ApiPermission;

/**
 * 
 * 
 * <br>
 * Class Name   : ApiPermssionHelper
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Oct 31, 2021
 */
public class ApiPermssionHelper {
	
	private static Logger logger = LoggerFactory.getLogger("com.mendmix.security");
	
	private static ApiPermission defaultApiOnNotMatch = new ApiPermission(null, null, PermissionLevel.LoginRequired);

	private static Map<String, ApiPermission> apiPermissions = new HashMap<>();
	private static Map<Pattern, ApiPermission> wildcardUris = new HashMap<>();
	
	public static final String WILDCARD_START = "{";
	private static final String PATH_VARIABLE_REGEX = "\\{[^/]+?\\}";
	private static final String LEAST_ONE_REGEX = ".+";
	
	
	public static void init(SecurityDecisionProvider decisionProvider) {
    	List<ApiPermission> permissions = decisionProvider.getAllApiPermissions();
    	String permissionKey;
    	for (ApiPermission apiPermission : permissions) {
    		if(apiPermission.getPermissionLevel() == PermissionLevel.LoginRequired)continue;
    		permissionKey = buildPermissionKey(apiPermission.getMethod(), decisionProvider.resolveUri(apiPermission.getUri()));
    		apiPermission.setPermissionKey(permissionKey);
    		apiPermissions.put(permissionKey, apiPermission);
    		if(permissionKey.contains("{")) {
    			Pattern pattern = Pattern.compile(pathVariableToPattern(permissionKey));
    			wildcardUris.put(pattern, apiPermission);
    		}
		}
    	//
    	List<ApiModel> anonymousUris = decisionProvider.anonymousUris();
    	for (ApiModel api : anonymousUris) {
    		permissionKey = buildPermissionKey(api.getMethod(), decisionProvider.resolveUri(api.getUri()));
    		ApiPermission apiPermission = new ApiPermission(api.getMethod(), api.getUri(), PermissionLevel.Anonymous);
			if(api.getUri().contains("*")) {
				Pattern pattern = Pattern.compile(pathVariableToPattern(permissionKey.replaceAll("\\*+", LEAST_ONE_REGEX)));
    			wildcardUris.put(pattern, apiPermission);
			}else {
				apiPermissions.put(permissionKey,apiPermission);
			}
		}
    	
    	if(apiPermissions.isEmpty()) {
    		apiPermissions.put("default", defaultApiOnNotMatch);
    	}
    	
    	logger.info(">>load apiPermissions:{}",apiPermissions.keySet());
    }
	
	public static ApiPermission matchPermissionObject(String method, String uri) {
		if(apiPermissions.isEmpty()) {
			synchronized (apiPermissions) {
				if(apiPermissions.isEmpty()) {
					init(SecurityDelegating.decisionProvider());
				}
			}
		}
		String permissionKey = buildPermissionKey(method, uri);
		ApiPermission api = apiPermissions.get(permissionKey);
		if(api != null) {
			return api;
		}
		for (Pattern pattern : wildcardUris.keySet()) {
			if(pattern.matcher(permissionKey).matches()) {
				return wildcardUris.get(pattern);
			}
		}
		return defaultApiOnNotMatch;
	}

	
	public static String buildPermissionKey(String method, String uri) {
		return new StringBuilder(method.toUpperCase()).append(GlobalConstants.UNDER_LINE).append(uri).toString();
	}
	
	public static String pathVariableToPattern(String uri) {
		return uri.replaceAll(PATH_VARIABLE_REGEX, LEAST_ONE_REGEX);
	}
}
