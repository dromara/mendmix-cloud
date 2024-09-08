/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.gateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;

import org.dromara.mendmix.common.exception.ForbiddenAccessException;
import org.dromara.mendmix.common.exception.UnauthorizedException;
import org.dromara.mendmix.common.model.AuthUser;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Jun 11, 2022
 */
public interface AuthorizationProvider {

	AuthUser doAuthorization(ServerHttpRequest request) throws UnauthorizedException,ForbiddenAccessException;
}
