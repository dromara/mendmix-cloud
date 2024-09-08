/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package org.dromara.mendmix.logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.common.util.TimeConvertUtils;
import org.springframework.http.HttpHeaders;

/**
 * 
 * <br>
 * Class Name   : LogConstants
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年9月26日
 */
public class LogConstants {

	public static final String BASIC = "basic";
	public static final String CONTEXT = "context";
	
    public static final String TRACE_ID = "traceId";
	public static final String ENV = "env";
	public static final String MAIN_SYSTEM_ID = "mainSystemId";
	public static final String SYSTEM_KEY = "systemKey";
	public static final String SYSTEM_ID = "systemId";
	public static final String SERVICE_ID = "serviceId";
	public static final String DEPLOY_GROUP = "deployGroup";
	public static final String DEPLOY_NAME = "deployName";
	public static final String POD_IP = "podIp";
	public static final String POD_NAME = "podName";
	public static final String TIME_ZONE = "timezone";
	public static final String TIME_OFFSET = "timeOffset";
	public static final String GATEWAY = "gateway";
	
	public static final String TENANT_ID = "tenantId";
	public static final String USER_NAME = "userName";
	public static final String USER_ID = "userId";
	public static final String SUBJECT_ID = "subjectId";
	public static final String PLATFORM_TYPE = "platformType";
	public static final String CLIENT_TYPE = "clientType";
	
	public static final String MESSAGE = "message";
	public static final String THROWN = "thrown";
	public static final String FILE_LINE = "fileLine";
	public static final String THREAD = "thread";
	public static final String LEVEL = "level";
	public static final String TIMESTAMP = "timestamp";
	
	
	public static final Map<String, Object> SERVICE_INFO = new LinkedHashMap<>();

	public final static List<String> LOGGING_REQUEST_HEADERS = new ArrayList<>(Arrays.asList(
			HttpHeaders.CONTENT_TYPE,
			CustomRequestHeaders.HEADER_TENANT_ID,
			CustomRequestHeaders.HEADER_REQUEST_ID,
			CustomRequestHeaders.HEADER_CLIENT_TYPE,
			CustomRequestHeaders.HEADER_SYSTEM_ID,
			CustomRequestHeaders.HEADER_BUSINESS_UNIT_ID,
			CustomRequestHeaders.HEADER_SERVICE_CHAIN,
			HttpHeaders.ACCEPT_LANGUAGE,
			CustomRequestHeaders.HEADER_TIME_ZONE,
			CustomRequestHeaders.HEADER_REFERER_PERM_GROUP,
			CustomRequestHeaders.HEADER_FORWARDED_GATEWAY,
			CustomRequestHeaders.HEADER_TRACE_LOGGING
		 ));
	
	public final static List<String> LOGGING_GATEWAY_REQUEST_HEADERS = new ArrayList<>(Arrays.asList(
			HttpHeaders.REFERER,
			HttpHeaders.USER_AGENT,
			CustomRequestHeaders.HEADER_FORWARDED_GATEWAY,
			CustomRequestHeaders.HEADER_SERVICE_CHAIN
		));
	
	public final static List<String> LOGGING_RESPONSE_HEADERS = new ArrayList<>(Arrays.asList(
			HttpHeaders.CONTENT_TYPE,
			HttpHeaders.CONTENT_LENGTH
		));
	
	public static enum LogSubType {
		userBehaviorLog,
		pageTraceLog,
		requestTraceLog,
		appBizLog,
		userEnvLog;
	}
	
	static {
		SERVICE_INFO.put(ENV, GlobalContext.ENV.toUpperCase());
		SERVICE_INFO.put(SYSTEM_KEY, GlobalContext.SYSTEM_KEY);
		SERVICE_INFO.put(SERVICE_ID, GlobalContext.APPID);
		SERVICE_INFO.put(GATEWAY, GlobalContext.isGateway());
		SERVICE_INFO.put(TIME_ZONE, TimeConvertUtils.localTimeZoneId);
		SERVICE_INFO.put(TIME_OFFSET, TimeConvertUtils.TIME_ZONE_OFFSET);
		SERVICE_INFO.put(POD_IP, ResourceUtils.getAnyProperty("POD_IP"));
		SERVICE_INFO.put(POD_NAME, ResourceUtils.getAnyProperty("POD_NAME"));
	}
}
