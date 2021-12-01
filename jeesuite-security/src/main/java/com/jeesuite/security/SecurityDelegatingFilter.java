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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.exception.ForbiddenAccessException;
import com.jeesuite.common.exception.UnauthorizedException;
import com.jeesuite.common.http.HttpMethod;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.WebUtils;
import com.jeesuite.security.model.UserSession;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月30日
 */
public class SecurityDelegatingFilter implements Filter {

	private static final String DOT = ".";
	private static final String MSG_401_UNAUTHORIZED = "{\"code\": 401,\"msg\":\"401 Unauthorized\"}";
	private static String MSG_403_FORBIDDEN = "{\"code\": 403,\"msg\":\"403 Forbidden\"}";
	
	private static String apiUriSuffix = ResourceUtils.getProperty("api.uri.suffix");
	
	private AuthAdditionHandler additionHandler;
	
	
	public void setAdditionHandler(AuthAdditionHandler additionHandler) {
		this.additionHandler = additionHandler;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		
		//忽略静态资源
		if(request.getRequestURI().contains(DOT) && (apiUriSuffix == null || !request.getRequestURI().endsWith(apiUriSuffix))) {
			chain.doFilter(req, res);
			return;
		}
		
		if(request.getMethod().equals(HttpMethod.OPTIONS.name())) {
			chain.doFilter(req, res);
			return;
		}
		
		if(additionHandler != null) {
			additionHandler.beforeAuthorization(request, response);
		}
		
		CurrentRuntimeContext.init(request, response);

		UserSession userSession = null;
		try {
			userSession = SecurityDelegating.doAuthorization();
		} catch (UnauthorizedException e) {
			if(WebUtils.isAjax(request)){				
				WebUtils.responseOutJson(response, MSG_401_UNAUTHORIZED);
			}else{
				if(SecurityDelegating.getConfigurerProvider().error401Page() == null){
					response.setStatus(401);
					WebUtils.responseOutHtml(response, "401 Unauthorized");
				}else{					
					String loginPage = WebUtils.getBaseUrl(request) + SecurityDelegating.getConfigurerProvider().error401Page();
					response.sendRedirect(loginPage);
				}
			}
			return;
		}catch (ForbiddenAccessException e) {
			if(WebUtils.isAjax(request)){				
				WebUtils.responseOutJson(response, MSG_403_FORBIDDEN);
			}else{
				if(SecurityDelegating.getConfigurerProvider().error403Page() == null){
					response.setStatus(403);
					WebUtils.responseOutHtml(response, "403 Forbidden");
				}else{					
					String loginPage = WebUtils.getBaseUrl(request) + SecurityDelegating.getConfigurerProvider().error403Page();
					response.sendRedirect(loginPage);
				}
			}
			return;
		}
		//
		if(additionHandler != null) {
			additionHandler.afterAuthorization(userSession);
		}
		
		chain.doFilter(req, res);
	}

	@Override
	public void destroy() {}

}
