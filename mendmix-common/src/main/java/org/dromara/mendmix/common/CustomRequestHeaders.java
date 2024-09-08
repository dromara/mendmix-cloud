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

public class CustomRequestHeaders {

	// header
	public static final String ACCEPT_LANGUAGE = "Accept-Language";
	public static final String HEADER_PREFIX = "x-";
	public static final String HEADER_REAL_IP = "x-real-ip";
	public static final String HEADER_FROWARDED_FOR = "x-forwarded-for";
	public static final String HEADER_INVOKE_TOKEN = "x-invoke-token";
	public static final String HEADER_ACCESS_TOKEN = "x-access-token";
	public static final String HEADER_AUTH_USER = "x-auth-user";
	public static final String HEADER_TENANT_ID = "x-tenant-id";
	public static final String HEADER_USER_ID = "x-user-id";
	public static final String HEADER_DATA_ID = "x-data-id";
	public static final String HEADER_CLIENT_TYPE = "x-client-type";
	public static final String HEADER_SYSTEM_ID = "x-system-id";
	public static final String HEADER_REQUESTED_WITH = "x-requested-with";
	public static final String HEADER_FORWARDED_HOST = "x-forwarded-host";
	public static final String HEADER_FORWARDED_PROTO = "x-forwarded-proto";
	public static final String HEADER_FORWARDED_PORT = "x-forwarded-port";
	public static final String HEADER_FORWARDED_PRIFIX = "x-forwarded-prefix";
	public static final String HEADER_SERVICE_CHAIN = "x-forwarded-service-chain";
	public static final String HEADER_FORWARDED_GATEWAY = "x-forwarded-gateway";
	public static final String HEADER_SESSION_ID = "x-session-id";
	public static final String HEADER_SESSION_EXPIRE_IN = "x-session-expire-in";
	public static final String HEADER_INVOKER_IP = "x-invoker-ip";
	public static final String HEADER_INTERNAL_REQUEST = "x-internal-request";
	public static final String HEADER_INVOKER_APP_ID = "x-invoker-appid";
	public static final String HEADER_INVOKER_IS_GATEWAY = "x-invoker-is-gateway";
	public static final String HEADER_REQUEST_ID = "x-request-id";
	public static final String HEADER_RESP_KEEP = "x-resp-keep";
	public static final String HEADER_HTTP_STATUS_KEEP = "x-httpstatus-keep";
	public static final String HEADER_ORIGIN_HTTP_STATUS = "x-original-httpstatus";
	public static final String HEADER_VERIFIED_MOBILE = "x-verified-mobile";
	public static final String HEADER_IGNORE_TENANT = "x-ignore-tenant";
	public static final String HEADER_IGNORE_AUTH = "x-ignore-auth";
	public static final String HEADER_CLUSTER_ID = "x-cluster-id";
	public static final String HEADER_EXCEPTION_CODE = "x-exception-code";
	public static final String HEADER_BUSINESS_UNIT_ID = "x-bunit-id";
	public static final String HEADER_TIME_ZONE = "x-timezone";
	public static final String HEADER_USING_GRAY_STRATEGY = "x-gray-strategy";
	public static final String HEADER_REFERER_PERM_GROUP = "x-referer-permGroup";
	public static final String HEADER_TRACE_LOGGING = "x-trace-logging";
	public static final String HEADER_REQUEST_BODY_LOGGING = "x-requestBody-logging";

	public static final String HEADER_OPEN_SIGN = "x-open-sign";
	public static final String HEADER_OPEN_APP_ID = "x-open-clientId";
	public static final String HEADER_SIGN_TYPE = "x-sign-type";
	public static final String HEADER_TIMESTAMP = "timestamp";
}
