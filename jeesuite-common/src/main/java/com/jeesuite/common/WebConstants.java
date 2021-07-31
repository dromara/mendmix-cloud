package com.jeesuite.common;

public class WebConstants {
	//
	public static final String DOT = ".";
	public static final String COLON = ":";
	public static final String MID_LINE = "-";
	public static final String UNDER_LINE = "_";
	public static final String COMMA = ",";
	public static final String AT = "@";
	public static String PATH_SEPARATOR = "/";
	// header
	public static final String HEADER_PREFIX = "x-";
	public static final String HEADER_REAL_IP = "x-real-ip";
	public static final String HEADER_FROWARDED_FOR = "x-forwarded-for";
	public static final String HEADER_AUTH_TOKEN = "x-auth-token";
	public static final String HEADER_AUTH_USER = "x-auth-user";
	public static final String HEADER_TENANT_ID = "x-tenant-id";
	public static final String HEADER_FORWARDED_BASE_URL = "x-forwarded-base-url";
	public static final String HEADER_REQUESTED_WITH = "x-requested-with";
	public static final String HEADER_FORWARDED_HOST = "x-forwarded-host";
	public static final String HEADER_FORWARDED_PROTO = "x-forwarded-proto";
	public static final String HEADER_FORWARDED_PORT = "x-forwarded-port";
	public static final String HEADER_FORWARDED_PRIFIX = "x-forwarded-prefix";
	public static final String HEADER_GATEWAY_TOKEN = "x-gateway-token";
	public static final String HEADER_SESSION_ID = "x-session-id";
	public static final String HEADER_SESSION_EXPIRE_IN = "x-session-expire-in";
	public static final String HEADER_INVOKER_IP = "x-invoker-ip";
	public static final String HEADER_INTERNAL_REQUEST = "x-internal-request";
    public static final String HEADER_INVOKER_APP_ID = "x-invoker-appid";
    public static final String HEADER_REQUEST_ID = "x-request-id";
	public static final String HEADER_RESP_KEEP = "x-resp-keep";
	public static final String HEADER_VERIFIED_MOBILE = "x-verified-mobile";
	
	public static final String PARAM_RETURN_URL = "returnUrl";
	public static final String PARAM_AUTH_CODE = "auth_code";
	public static final String PARAM_CODE = "code";
	public final static String PARAM_DATA = "data";
	public final static String PARAM_SIGN = "sign";
	
    public static final String MSG_401_UNAUTHORIZED = "{\"code\": 401,\"msg\":\"401 Unauthorized\"}";
	
	public static final String MSG_403_FORBIDDEEN = "{\"code\": 403,\"msg\":\"403 forbidden\"}";
}
