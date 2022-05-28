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
package com.mendmix.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.mendmix.common.GlobalConstants;
import com.mendmix.common.constants.PermissionLevel;

/**
 * 
 * 
 * <br>
 * Class Name   : ApiPermssionCheckHelper
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Oct 31, 2021
 */
public class ApiPermssionCheckHelper {

	private  static Map<String, Pattern> wildcardPermissionPatterns = new HashMap<>();
	
	public static final String WILDCARD_START = "{";
	private static final String PATH_VARIABLE_REGEX = "\\{[^/]+?\\}";
	private static final String LEAST_ONE_REGEX = ".+";
	
	/**
	 * 判断当前检查类型
	 * @param resourceManager
	 * @param permissionKey
	 * @return
	 */
	public static PermissionLevel matchPermissionLevel(SecurityResourceManager resourceManager,String permissionKey){
		
		List<String> uris = resourceManager.getAnonUris();
		if(uris != null && uris.contains(permissionKey))return PermissionLevel.Anonymous;
		
		List<Pattern> patterns = resourceManager.getAnonUriPatterns();
		if(patterns != null){
			for (Pattern pattern : patterns) {
				if(pattern.matcher(permissionKey).matches())return PermissionLevel.Anonymous;
			}
		}
		
		uris = resourceManager.getAuthzUris();
		if(uris != null && uris.contains(permissionKey))return PermissionLevel.PermissionRequired;
		
		patterns = resourceManager.getAuthzPatterns();
		if(patterns != null){			
			for (Pattern pattern : patterns) {
				if(pattern.matcher(permissionKey).matches())return PermissionLevel.PermissionRequired;
			}
		}
		
		return PermissionLevel.LoginRequired;
	}
	
	/**
	 * 检查权限
	 * @param permissionKey
	 * @param routeName
	 * @param permissions
	 * @return
	 */
	public static boolean checkPermissions(SecurityResourceManager resourceManager,String permissionKey,List<String> permissions){
		if(permissions == null)return false;
		if(permissions.contains(permissionKey))return true;
		
		//如果这个uri是不带通配符的情况
        if(resourceManager.getNonWildcardUris().contains(permissionKey) && !permissions.contains(permissionKey)){
        	return false;
        }
        
        //通配符匹配
		for (String per : permissions) {
			Pattern pattern = wildcardPermissionPatterns.get(per);
			if(pattern == null){
				synchronized (wildcardPermissionPatterns) {
					pattern = wildcardPermissionPatterns.get(per);
					if(pattern == null){
						pattern = Pattern.compile(per);
					}
				}
			}
			
			if(pattern.matcher(permissionKey).matches()){
				return true;
			}
		}
		return false;
	}
	
	public static String buildPermissionKey(String method, String uri) {
		return new StringBuilder(method.toUpperCase()).append(GlobalConstants.UNDER_LINE).append(uri).toString();
	}
	
	public static String pathVariableToPattern(String uri) {
		return uri.replaceAll(PATH_VARIABLE_REGEX, LEAST_ONE_REGEX);
	}
}
