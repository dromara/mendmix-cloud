package com.jeesuite.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.jeesuite.common.GlobalConstants;
import com.jeesuite.common.constants.PermissionLevel;

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
			if(!per.contains(SecurityResourceManager.WILDCARD_START))continue;
			
			Pattern pattern = wildcardPermissionPatterns.get(per);
			if(pattern == null){
				synchronized (wildcardPermissionPatterns) {
					pattern = wildcardPermissionPatterns.get(per);
					if(pattern == null){
						pattern = Pattern.compile(per.replaceAll("\\{[^/]+?\\}", ".+"));
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
}
