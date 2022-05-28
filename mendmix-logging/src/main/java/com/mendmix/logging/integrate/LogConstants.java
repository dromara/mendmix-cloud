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
package com.mendmix.logging.integrate;

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

	public static final String LOG_CONTEXT_ENV = "env";
	public static final String LOG_CONTEXT_APP_ID = "appId";
	public static final String LOG_CONTEXT_REQUEST_IP = "requestIp";
	public static final String LOG_CONTEXT_REQUEST_ID = "requestId";
	public static final String LOG_CONTEXT_CURRENT_USER = "currentUser";
	public static final String LOG_CONTEXT_INVOKER_APP_ID = "invokerAppId";
	
	public static final String PARAM_LOG_TYPE = "logType";

	public static final String USER_ACTION_LOG_TOPIC = "user-action-logs";
	
	public static final String APP_UNION_LOG_TOPIC = "app-union-logs";
	
	public static enum LogSubType {
		userBehaviorLog,
		pageTraceLog,
		requestTraceLog,
		appBizLog,
		userEnvLog;
	}
}
