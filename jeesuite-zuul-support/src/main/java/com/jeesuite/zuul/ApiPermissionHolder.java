package com.jeesuite.zuul;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.constants.PermissionLevel;
import com.jeesuite.zuul.model.ApiPermission;
import com.jeesuite.zuul.model.BizSystemModule;



/**
 * 权限资源管理器
 * <br>
 * Class Name   : ApiPermissionHolder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年3月6日
 */
public class ApiPermissionHolder {



	private static Logger log = LoggerFactory.getLogger(ApiPermissionHolder.class);

	// Authorization  Authentication
	public static final String WILDCARD_START = "{";
	private static final String UNDER_LINE = "_";
	private static Map<String, List<String>> authzUris = new HashMap<>();
	private static Map<String, List<Pattern>> authzPatterns = new HashMap<>();

	private static Map<String, List<String>> anonUris = new HashMap<>();
	private static Map<String, List<Pattern>> anonUriPatterns = new HashMap<>();
	private static Map<String, Pattern> innerOnlyUris = new HashMap<>();
	
	// 所有无通配符uri
	private static Map<String, List<String>> nonWildcardUris = new HashMap<>();
	
	public static Map<String, List<String>> getAuthorizationUris() {
		return authzUris;
	}

	public static Map<String, List<Pattern>> getAuthorizationUriPatterns() {
		return authzPatterns;
	}

	public static Map<String, List<String>> getAnonymousUris() {
		return anonUris;
	}

	public static Map<String, List<Pattern>> getAnonymousUriPatterns() {
		return anonUriPatterns;
	}

	public static Map<String, Pattern> getInnerOnlyUris() {
		return innerOnlyUris;
	}

	public static List<String> getNonWildcardUris(String routeName) {
		return nonWildcardUris.get(routeName);
	}
	
	public static synchronized void loadApiPermissions() {
		
		List<BizSystemModule> modules = CurrentSystemHolder.getModules();
		log.info("============begin load perssion data==============");
		List<ApiPermission> permissionInfos = null;
		try {
			boolean withWildcard;
			String permissionKey;
			for (BizSystemModule module : modules) {
				nonWildcardUris.put(module.getRouteName(), new ArrayList<>());
				permissionInfos = ServiceInstances.systemMgrApi().findApiPermission(module.getAppId());
				if(permissionInfos == null || permissionInfos.isEmpty())continue;
				for (ApiPermission permissionInfo : permissionInfos) {
					permissionInfo.setRouteName(module.getRouteName());
					withWildcard = permissionInfo.getUri().contains(WILDCARD_START);
					if(!withWildcard){
						permissionKey = buildPermissionKey(permissionInfo.getHttpMethod(), permissionInfo.getUri());
						nonWildcardUris.get(module.getRouteName()).add(permissionKey);
					}
					if(PermissionLevel.PermissionRequired.name().equals(permissionInfo.getGrantType())){
						if (withWildcard) {
							putUriPattern(authzPatterns, permissionInfo);
						} else {
							putUri(authzUris, permissionInfo);
						}
					}else if(PermissionLevel.Anonymous.name().equals(permissionInfo.getGrantType())){
						if (withWildcard) {
							putUriPattern(anonUriPatterns, permissionInfo);
						} else {
							putUri(anonUris, permissionInfo);
						}
					}
				}
			}
			
			log.info("nonWildcardUris:         {}",nonWildcardUris);
			log.info("anonUris:                {}",anonUris);
			log.info("anonUriPatterns:         {}",anonUriPatterns);
			log.info("authzUris:               {}",authzUris);
			log.info("authzPatterns:           {}",authzPatterns);
			log.info("============load perssion data finish==============");
		} catch (Exception e) {
			log.error("load permission data error",e);
		}

	}

	private static void putUri(Map<String, List<String>> map, ApiPermission permissionInfo) {
		String permKey = buildPermissionKey(permissionInfo.getHttpMethod(), permissionInfo.getUri());
		List<String> list = map.get(permissionInfo.getRouteName());
		if (list == null) {
			list = new ArrayList<>();
			map.put(permissionInfo.getRouteName(), list);
		}
		list.add(permKey);
	}

	private static void putUriPattern(Map<String, List<Pattern>> map, ApiPermission permissionInfo) {
		String permKey = buildPermissionKey(permissionInfo.getHttpMethod(), permissionInfo.getUri());
		List<Pattern> list = map.get(permissionInfo.getRouteName());
		if (list == null) {
			list = new ArrayList<>();
			map.put(permissionInfo.getRouteName(), list);
		}

		Pattern pattern = Pattern.compile(permKey.replaceAll("\\{[^/]+?\\}", ".+"));
		list.add(pattern);
	}
	
	public static String buildPermissionKey(String method,String uri){
		return new StringBuilder(method.toUpperCase()).append(UNDER_LINE).append(uri).toString();
	}
}
