/**
 * 
 */
package com.jeesuite.rest;

import com.jeesuite.common.util.ResourceUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年1月19日
 * @Copyright (c) 2015, vakinge@github
 */
public class RestConst {

	/**
	 * 
	 */
	public static final String REQUEST_ID_PRAMS_NAME = "requestId";
	public static final String ACCESSTOKEN_PRAMS_NAME = "accessToken";
	public static final String APP_TYPE_PRAMS_NAME = "appType";
	public static final String CLIENTID_PRAMS_NAME = "clientId";
	public static final String USERID_PRAMS_NAME = "userId";
	public static final String DOCTOR_PRAMS_NAME = "doctorId";
	public static final String APPVER_PRAMS_NAME = "version";
	public static final String OS_PRAMS_NAME = "systemType";
	
	public static final String APP_AREA_NAME = "area";
	public static final String APP_LANGUAGE_NAME = "language";

	public static final String POST_METHOD = "POST";
	public static final String GET_METHOD = "GET";
	
	public static final String PROP_REQUEST_BEGIN_TIME = "request_start_time";
	
	 /**
     * Part of HTTP content type header.
     */
    public static final String MULTIPART = "multipart/";

    /**
     * HTTP content type header for multipart forms.
     */
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    
    public static final String SERVICENAME_PROP_NAME = "serviceName";
    
    
    public static final String ACCESS_CONTROL_ALLOW_METHODS_TITLE = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "POST, GET,DELETE,PUT, OPTIONS";
	
    public static final String ALLOW_HEADERS = "Origin, X-Requested-With, Content-Type, Accept, Cookie";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    /**
	 * 慢应用跟踪阀值（毫秒）
	 */
    public static final long SLOW_THRESHOLD = Long.parseLong(ResourceUtils.getProperty("slow_request_threshold", "10000"));

}
