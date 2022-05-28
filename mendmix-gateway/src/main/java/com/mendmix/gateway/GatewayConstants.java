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
package com.mendmix.gateway;

public class GatewayConstants {

	public static final String CONTEXT_REQUEST_START_TIME = "ctx-req-startTime";
	public static final String CONTEXT_ROUTE_SERVICE = "ctx-route-svc";
	public static final String CONTEXT_IGNORE_FILTER = "ctx-ignore-filter";
	public static final String CONTEXT_TRUSTED_REQUEST = "ctx-trusted-req";
	public static final String CONTEXT_REQUEST_HIT_KEY = "ctx-req-hitKey";
	public static final String CONTEXT_CURRENT_API = "ctx-current-api";
	public static final String CACHED_REQUEST_BODY_STR_ATTR = "cachedRequestBodyStr";
	
	public static final String X_SIGN_HEADER = "x-open-sign";
	public static final String APP_ID_HEADER = "x-open-appId";
	public static final String TIMESTAMP_HEADER = "timestamp";
	
	public static final String PATH_PREFIX = "/api";
}
