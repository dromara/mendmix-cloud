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
package com.mendmix.common;

public class GlobalConstants {
	//
	public static final String ASTERISK = "*";
	public static final String DOT = ".";
	public static final String COLON = ":";
	public static final String MID_LINE = "-";
	public static final String UNDER_LINE = "_";
	public static final String COMMA = ",";
	public static final String AT = "@";
	public static String PATH_SEPARATOR = "/";

	public static final String PARAM_RETURN_URL = "returnUrl";
	public static final String PARAM_AUTH_CODE = "auth_code";
	public static final String PARAM_CODE = "code";
	public final static String PARAM_MSG = "msg";
	public final static String PARAM_DATA = "data";
	public final static String PARAM_SIGN = "sign";
	
	public static final String PLACEHOLDER_PREFIX = "${";
	public static final String PLACEHOLDER_SUFFIX = "}";
	
    public static final String MSG_401_UNAUTHORIZED = "{\"code\": 401,\"msg\":\"401 Unauthorized\"}";
	
	public static final String MSG_403_FORBIDDEEN = "{\"code\": 403,\"msg\":\"403 forbidden\"}";
	
    public static final String IGNORE_PLACEHOLER = "[Ignore]";
	
	public static final String CRYPT_PREFIX = "{Cipher}";
	
	public static final String DEFAULT_EXT_FIELDS_NAME = "extFields";
	public static final String DEFAULT_EXT_VALUES_NAME = "extValues";
	
	public static final String FEIGN_CLIENT = "feign-client";
}
