package com.jeesuite.security.client;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.jeesuite.security.SecurityConstants;
import com.jeesuite.security.model.UserSession;
import com.jeesuite.springweb.RequestContextHelper;
import com.jeesuite.springweb.exception.UnauthorizedException;


public class LoginContext {
	
	private final static ThreadLocal<UserSession> holder = new ThreadLocal<>();
	
	private static HttpServletRequest getHttpRequest(){
		HttpServletRequest request = RequestContextHelper.getRequest();
		if(request == null){
		  request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		}
		
		return request;
	}

	public static UserSession getUserSession(){
		UserSession loginSession = holder.get();
		if(loginSession == null){
			String headerString = getHttpRequest().getHeader(SecurityConstants.HEADER_AUTH_USER);
			loginSession = UserSession.decode(headerString);
			//由于线程复用导致部分没有响应的请求没有清掉线程变量，故去掉
//			if(loginSession != null){
//				holder.set(loginSession);
//			}
		}
		return loginSession;
	}
	
	public static void setUserSession(UserSession loginSession){
		if(loginSession == null){
			holder.remove();
		}else{			
			holder.set(loginSession);
		}
	}
	
	public static void resetUserSessionHolder(){
		holder.remove();
	}
	
	/**
	 * 获取登录信息，未登录抛出异常
	 * @return
	 */
	public static UserSession getAndValidateCurrentSession(){
		UserSession loginInfo = getUserSession();
		if(loginInfo == null || loginInfo.isAnonymous())throw new UnauthorizedException();
		return loginInfo;
	}
	
	public static String getUserName(){
		return getAndValidateCurrentSession().getUserName();
	}
	
	public static String getUserId(){
		return getAndValidateCurrentSession().getUserId();
	}
	
	public static Integer getIntFormatUserId(){
		return Integer.parseInt(getAndValidateCurrentSession().getUserId());
	}
	
	public static Long getLongFormatUserId(){
		return Long.parseLong(getAndValidateCurrentSession().getUserId());
	}

}
