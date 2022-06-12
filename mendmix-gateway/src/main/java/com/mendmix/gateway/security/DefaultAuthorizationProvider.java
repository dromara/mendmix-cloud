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
package com.mendmix.gateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.exception.ForbiddenAccessException;
import com.mendmix.common.exception.UnauthorizedException;
import com.mendmix.common.model.AuthUser;
import com.mendmix.security.SecurityDelegating;
import com.mendmix.security.context.ReactiveRequestContextAdapter;
import com.mendmix.security.model.UserSession;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Jun 11, 2022
 */
public class DefaultAuthorizationProvider implements AuthorizationProvider {

	@Override
	public void initContext(ServerHttpRequest request) {
		ReactiveRequestContextAdapter.init(request);
	}

	@Override
	public AuthUser doAuthorization(String method, String uri)
			throws UnauthorizedException, ForbiddenAccessException {
		final UserSession session = SecurityDelegating.doAuthorization(method, uri);
		if(session != null && !session.isAnonymous()) {
			CurrentRuntimeContext.setAuthUser(session.getUser());
			if(session.getTenanId() != null) {
				CurrentRuntimeContext.setTenantId(session.getTenanId());
			}
			return session.getUser();
		}
		return null;
	}

}
