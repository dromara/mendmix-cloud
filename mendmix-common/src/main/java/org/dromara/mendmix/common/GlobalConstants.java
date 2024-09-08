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
package org.dromara.mendmix.common;

import org.apache.commons.lang3.StringUtils;

public class GlobalConstants {
	//
	public static final String ASTERISK = "*";
	public static final String DOT = ".";
	public static final String COLON = ":";
	public static final String MID_LINE = "-";
	public static final String UNDER_LINE = "_";
	public static final String COMMA = ",";
	public static final String SEMICOLON = ";";
	public static final String AT = "@";
	public static String PATH_SEPARATOR = "/";
	public static String BACK_SLASH = "\\";
	public static String BRACES_OPEN = "{";
	public static String BRACES_CLOSE = "}";
	public static final String EQUALS = "=";
	public static final String SCHEME_SPLITER = "://";
	public static final String PARAM_SEPARATOR = "&";

	public static final String PARAM_RETURN_URL = "returnUrl";
	public static final String PARAM_AUTH_CODE = "auth_code";
	public static final String PARAM_MSG = "msg";
	public static final String PARAM_CODE = "code";
	public static final String PARAM_ERROR = "error";
	public static final String PARAM_ERROR_DESC = "error_description";
	public static final String PARAM_BIZ_CODE = "bizCode";
	public final static String PARAM_DATA = "data";
	public final static String PARAM_SIGN = "sign";
	public final static String PARAM_SYSTEM_ID = "systemId";
	public final static String PARAM_TENANT_ID = "tenantId";
	public final static String PARAM_CMD = "cmd";
	public final static String PARAM_PAYLOAD = "payload";
	public final static String PARAM_TOKEN = "token";
	
	public final static String ENV_LOCAL = "local";
	public final static String ENV_DEV = "dev";
	public final static String ENV_PRD = "prd";
	
	public final static String LOCAL_MOCK_CLUSTER_ID = "local_mock";
	
	public final static String DEFAULT_SYSTEM_ID = "1";
	
	public final static String VIRTUAL_TENANT_ID = "-1";
	
	public static final String IGNORE_PLACEHOLER = "[Ignore]";
	
	public static final String CRYPT_PREFIX = "{Cipher}";
	public static final String DEFAULT_CRIPT_KEY = StringUtils.join("mendmix","#d$@!","123");
	public static final String DEFAULT_EXT_BIZ_NAME = "ext_biz_type";
	public static final String DEFAULT_EXT_FIELDS_NAME = "extFields";
	public static final String DEFAULT_EXT_VALUES_NAME = "extValues";
	public static final String DEFAULT_EXT_FIELD_VERSION = "extVersion";
	
	public static final String DEBUG_TRACE_PARAM_NAME = "_debug_trace_context";
	
	public static final String CONTEXT_TOKEN_PARAM_NAME = "_context_token";
	public static final String CONTEXT_EXCEPTION = "_context_exception";
	public static final String CONTEXT_REQUEST_URL_KEY = "_context-request-url";
	public static final String CONTEXT_REQUEST_BODY_KEY = "_context-request-body-caching";
	public static final String CONTEXT_CURRENT_API_KEY = "_cxt_current_api_";
	
	public static final String FEIGN_USER_AGENT_NAME = "feign";
	public static final String BACKEND_USER_AGENT_NAME = "backend-client";
	
	public static final String PROP_SPRING_APPLICATION_NAME = "spring.application.name";
	
	
	public static final String DEFAULT_DATA_PERM_SCOPE = "standard";
}
