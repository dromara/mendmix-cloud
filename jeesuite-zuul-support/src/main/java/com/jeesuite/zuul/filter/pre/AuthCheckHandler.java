package com.jeesuite.zuul.filter.pre;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.constants.PermissionLevel;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.zuul.ApiPermissionHolder;
import com.jeesuite.zuul.UserSessionManager;
import com.jeesuite.zuul.filter.FilterHandler;
import com.jeesuite.zuul.model.BizSystemModule;
import com.jeesuite.zuul.model.UserSession;
import com.jeesuite.springweb.CurrentRuntimeContext;
import com.jeesuite.springweb.exception.ForbiddenAccessException;
import com.jeesuite.springweb.exception.UnauthorizedException;
import com.netflix.zuul.context.RequestContext;

/**
 * 
 * 
 * <br>
 * Class Name   : AuthCheckHandler
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2020年9月15日
 */
public class AuthCheckHandler implements FilterHandler {
	
	private  Map<String, Pattern> wildcardPermissionPatterns = new HashMap<>();

	public AuthUser process(RequestContext ctx,HttpServletRequest request,BizSystemModule module) {

		UserSession session = UserSessionManager.getUserSession();
		//权限编码
		String permissionKey = ApiPermissionHolder.buildPermissionKey(request.getMethod() , StringUtils.substringAfter(request.getRequestURI(), module.getRouteName()));
				
		PermissionLevel apiPermissionLevel = null;  
		//查询接口权限数据
		if(module != null && module.getAnonUriMatcher() != null && module.getAnonUriMatcher().match(request.getRequestURI())){
			apiPermissionLevel = PermissionLevel.Anonymous;
		}

		if(apiPermissionLevel == null){
			apiPermissionLevel = matchPermissionLevel(permissionKey, module.getRouteName()); 
		}
		
		if(session == null){
			if(apiPermissionLevel == null || apiPermissionLevel != PermissionLevel.Anonymous){
				throw new UnauthorizedException();
			}
		}else{
			//如果需鉴权
			if(apiPermissionLevel == PermissionLevel.PermissionRequired){
				if(!checkPermissions(permissionKey, module.getRouteName(), session.getPermissions())){
					throw new ForbiddenAccessException();
				}
			}
		}
		
		if(session != null) {
			//
			CurrentRuntimeContext.setAuthUser(session.getUser());
			return session.getUser();
		}
		return null;
	}
	
	/**
	 * 判断当前检查类型
	 * @param permissionKey
	 * @param routeName
	 * @return
	 */
	private PermissionLevel matchPermissionLevel(String permissionKey,String routeName){
		
		List<String> uris = ApiPermissionHolder.getAnonymousUris().get(routeName);
		if(uris != null && uris.contains(permissionKey))return PermissionLevel.Anonymous;
		
		List<Pattern> patterns = ApiPermissionHolder.getAnonymousUriPatterns().get(routeName);
		if(patterns != null){
			for (Pattern pattern : patterns) {
				if(pattern.matcher(permissionKey).matches())return PermissionLevel.Anonymous;
			}
		}
		
		uris = ApiPermissionHolder.getAuthorizationUris().get(routeName);
		if(uris != null && uris.contains(permissionKey))return PermissionLevel.PermissionRequired;
		
		patterns = ApiPermissionHolder.getAuthorizationUriPatterns().get(routeName);
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
	private boolean checkPermissions(String permissionKey,String routeName,List<String> permissions){
		if(permissions == null)return false;
		if(StringUtils.isBlank(routeName))return true;
		if(permissions.contains(permissionKey))return true;
		
		//如果这个uri是不带通配符的情况
        if(ApiPermissionHolder.getNonWildcardUris(routeName).contains(permissionKey) && !permissions.contains(permissionKey)){
        	return false;
        }
        
        //通配符匹配
		for (String per : permissions) {
			if(!per.startsWith(routeName))continue;
			if(!per.contains(ApiPermissionHolder.WILDCARD_START))continue;
			
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

	@Override
	public int order() {
		return 0;
	}
}
