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
package com.mendmix.security.context;

import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.GlobalConstants;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.security.RequestContextAdapter;

/**
 * 
 * <br>
 * Class Name   : ServletRequestContextAdapter
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date May 14, 2022
 */
public class ServletRequestContextAdapter implements RequestContextAdapter {

	

	public static void init(HttpServletRequest request,HttpServletResponse response) {
		ThreadLocalContext.unset();
		ThreadLocalContext.set(_CTX_REQUEST_KEY, request);
		ThreadLocalContext.set(_CTX_RESPONSE_KEY, response);
		//
		Enumeration<String> headerNames = request.getHeaderNames();
		String headerName;
		while(headerNames.hasMoreElements()) {
			headerName = headerNames.nextElement();
			CurrentRuntimeContext.addContextHeader(headerName, request.getHeader(headerName));
		}
	}

	@Override
	public String getHeader(String headerName) {
		HttpServletRequest request = ThreadLocalContext.get(_CTX_REQUEST_KEY);
		return request.getHeader(headerName);
	}

	@Override
	public String getCookie(String cookieName) {
		HttpServletRequest request = ThreadLocalContext.get(_CTX_REQUEST_KEY);
		Cookie[] cookies = request.getCookies();
		for (Cookie cookie : cookies) {
			if(cookie.getName().equalsIgnoreCase(cookieName)) {
				return cookie.getValue();
			}
		}
		return null;
	}

	@Override
	public void addCookie(String domain, String cookieName, String cookieValue, int expire) {
		Cookie cookie = new Cookie(cookieName, cookieValue);
		cookie.setDomain(domain);
		cookie.setPath(GlobalConstants.PATH_SEPARATOR);
		cookie.setHttpOnly(true);
		if (expire >= 0) {
			cookie.setMaxAge(expire);
		}
		HttpServletResponse response = ThreadLocalContext.get(_CTX_RESPONSE_KEY);
		response.addCookie(cookie);
	}

}
