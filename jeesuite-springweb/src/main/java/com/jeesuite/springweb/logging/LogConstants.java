package com.jeesuite.springweb.logging;

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
