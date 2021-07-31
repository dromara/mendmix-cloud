package com.jeesuite.zuul;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.TokenGenerator;

/**
 * 
 * <br>
 * Class Name   : SessionCookieUtil
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年3月23日
 */
public class SessionCookieUtil {

	public static final String USER_TOKEN_NAME = "x-user-token";
	public static final String APP_SESSION_ID_NAME = ResourceUtils.getProperty("spec.session-id.name","JSESSIONID");
	public static final int SESSION_EXPIRE_IN = ResourceUtils.getInt("auth.session.expire.seconds", 7200);
	
	public static String getSessionId(HttpServletRequest request,HttpServletResponse response){
		String sessionId = request.getHeader(USER_TOKEN_NAME);
		if(StringUtils.isNotBlank(sessionId) && sessionId.length() >= 32){
			return sessionId;
		}
		
		sessionId = null;
		final Cookie[] oCookies = request.getCookies();
        if (oCookies != null) {
            for (final Cookie item : oCookies) {
                final String name = item.getName();
                if (APP_SESSION_ID_NAME.equals(name)) {
                    return item.getValue();
                }
            }
        }
        //
        if(response != null){	
        	sessionId = TokenGenerator.generate();
			Cookie cookie = createSessionCookies(request,sessionId, SESSION_EXPIRE_IN);
			response.addCookie(cookie);
		}
        return sessionId;
	}
	
	
	
   public String destroySessionCookies(HttpServletRequest request,HttpServletResponse response) {
		String sessionId = getSessionId(request,response);
		if(StringUtils.isNotBlank(sessionId)){
			response.addCookie(createSessionCookies(request,StringUtils.EMPTY, 0));
		}
		return sessionId;
	}
   
   private static Cookie createSessionCookies(HttpServletRequest request,String sessionId,int expire){
	   String host = request.getServerName();
		if(request.getServerPort() != 80 && request.getServerPort() != 443){
			host = host + ":" + request.getServerPort();
		}
		Cookie cookie = new Cookie(APP_SESSION_ID_NAME,sessionId);  
		cookie.setDomain(host);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		if(expire >= 0){			
			cookie.setMaxAge(expire);
		}
		return cookie;
	}
}
