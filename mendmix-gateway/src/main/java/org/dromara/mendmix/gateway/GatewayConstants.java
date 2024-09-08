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
package org.dromara.mendmix.gateway;

public class GatewayConstants {

	public static final String DEFAULT_PATH_PREFIX = "/api";
	public static final String LB_SCHAME = "lb://";
	public static final String HTTP_SCHAME = "http";
	
	public static final String CONTEXT_NAME_PREFIX = "ctx-";
	public static final String CONTEXT_REQUEST_START_TIME = "ctx-start-time";
	public static final String CONTEXT_IGNORE_FILTER = "ctx-ignore-filter";
	public static final String CONTEXT_TRUSTED_REQUEST = "ctx-trusted-request";
	public static final String CONTEXT_TOKEN_VALID = "ctx-token-valid";
	public static final String CONTEXT_EXTRA_RESP_MESSAGE = "_ctx_exrea_respMsg";
	public static final String CONTEXT_CURRENT_IP = "ctx-current-ip";
	public static final String CONTEXT_CURRENT_API = "ctx-current-api";
	public static final String CONTEXT_CURRENT_MODULE = "ctx-current-module";
	public static final String CONTEXT_CURRENT_ROUTE = "ctx-current-route";
	public static final String CONTEXT_CURRENT_HIT_KEY = "ctx-current-hitKey";
	public static final String CONTEXT_CURRENT_ORTIGN_URI = "ctx-current-uri";
	public static final String CONTEXT_CROSS_CLOUD_REQUEST = "ctx-cross-cloud-request";
	public static final String CONTEXT_CROSS_CLOUD_INPUT = "ctx-cross-cloud-input";
	public static final String CONTEXT_CROSS_CLOUD_OUTPUT = "ctx-cross-cloud-output";
	public static final String CONTEXT_CROSS_CLOUD_TRUSTED = "ctx-cross-cloud-trusted";
	public static final String CONTEXT_ABORT_REQUEST = "ctx-abort-request";
	public static final String CONTEXT_RESP_CONTENT_LENGTH = "ctx-resp-content-length";
	public static final String CONTEXT_USER_SESSION = "ctx-user-session";
	public static final String CONTEXT_CLEARED_CONTEXT_ATTR = "ctx-clearContextAttributes";
	public static final String CONTEXT_OPENAPI_ACTIVE = "ctx-openapi-active";
	public static final String CONTEXT_GRAY_ROUTED = "ctx-grayRouted";
	public static final String CONTEXT_REQUEST_BODY_SIZE = "ctx-request-body-size";
	public static final String CONTEXT_RESPONSE_BODY_SIZE = "ctx-response-body-size";
	public static final String CONTEXT_ACTIVE_LOG_BODY = "ctx-active-logBody";
	public static final String CONTEXT_FORCE_WEBAPI = "ctx-force-webapi";
	public static final String CONTEXT_IGNORE_API_PERM = "ctx-ignore-apiperm";
	public static final String CONTEXT_BACKEND_REQUEST = "ctx-backend-request";
	
	public static final String WEBAPI_SIGN_HEADER = "x-sign";
	
	public static final String SIGN_CACHE_PREFIX = "signKeep:";
	
	public static final String NULL_BODY_KEY = "__NULL_BODY_";
	
	//Invalid
	public static final String EXTRA_EVENT_HEADER = "x-extra-events";
	
	public static final String STATIC_RULE_ID_PREFIX = "static_rule_";
	
	public static final String UNDEFINED = "undefined";
	
	
	public static enum UserClientType {
		pc,app,miniProgram,wxh5
	}
	
	public static enum FallbackStrategy {
	    hitCache,throwException,returnBlank,forwardBackup,returnJson;
	}
	
	public static enum HitType {
		any,clientIp,user,tenant,system;
	}
	
}
