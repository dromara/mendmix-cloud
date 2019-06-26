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

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年12月3日
 */
public class SecurityConstants {

	
	public static final String DEFAULT_PROFILE = "default";
	
	public static final String ACCESSTOKEN = "access_token";

	// parameter
	public static final String PARAM_CLIENT_ID = "client_id";
	public static final String PARAM_RETURN_URL = "return_url";
	public static final String PARAM_SESSION_ID = "session_id";
	public static final String PARAM_LOGIN_TYPE = "login_type";
	public static final String PARAM_CODE = "code";
	public static final String PARAM_EXPIRE_IN = "expires_in";
	public static final String PARAM_TICKET = "ticket";
	public static final String PARAM_ACT = "act";

	// header
	public static final String HEADER_AUTH_USER = "x-auth-user";
	public static final String HEADER_AUTH_PROFILE = "x-auth-profile";
	public static final String JSONP_LOGIN_CALLBACK_FUN_NAME = "jsonpLoginCallback";
	public static final String JSONP_SETCOOKIE_CALLBACK_FUN_NAME = "jsonpSetCookieCallback";
	//config
	public static final String CONFIG_OAUTH2_TOKEN_EXPIRE_IN = "security.oauth2.access-token.expirein";
	public static final String CONFIG_STORAGE_TYPE = "security.cache.storage-type";

	// emum
	public enum CacheType {
		redis, local
	}
}
