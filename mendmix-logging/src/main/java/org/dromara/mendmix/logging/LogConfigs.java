/**
 * 
 */
package org.dromara.mendmix.logging;

import org.dromara.mendmix.common.util.ResourceUtils;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Sep 7, 2024
 */
public class LogConfigs {

	public static final boolean API_LOGGING = ResourceUtils.getBoolean("mendmix-cloud.logging.apilog.enabled");
	public static long API_LOG_BODY_LIMIT = ResourceUtils.getLong("mendmix-cloud.logging.apilog.responseBody.limit", 256 * 1024);
	public static boolean API_LOG_IGNORE_PAGE_QUERY_RESP_BODY = ResourceUtils.getBoolean("mendmix-cloud.logging.apilog.responseBody.ignorePageQuery", true);
	public static boolean API_LOG_IGNORE_SUB_GATEWAY = ResourceUtils.getBoolean("mendmix-cloud.logging.apilog.ignoreSubGateway", true);
	
	public static final boolean TRACE_LOGGING = ResourceUtils.getBoolean("mendmix-cloud.logging.tracelog.enabled");
	public static final boolean TRACE_LOGGING_DYNA_MODE = TRACE_LOGGING && ResourceUtils.getBoolean("mendmix-cloud.logging.tracelog.dynamicMode",true);
	public static final double TRACE_LOGGING_USE_TIME_OVER_RATE = Double.parseDouble(ResourceUtils.getProperty("mendmix-cloud.logging.tracelog.useTimeOverRate", "0.3"));
	public static final int TRACE_LOGGING_USE_TIME_OVER = ResourceUtils.getInt("mendmix-cloud.logging.tracelog.useTimeOverMillis", 200);
	public static final int TRACE_LOGGING_MAX_INVERTAL = ResourceUtils.getInt("mendmix-cloud.logging.tracelog.maxInterval", 30000);
	public static final int TRACE_LOGGING_MAX_NUMS = ResourceUtils.getInt("mendmix-cloud.logging.tracelog.maxNums", 3);
	public static final int TRACE_LOGGING_TIME_THRESHOLD = ResourceUtils.getInt("mendmix-cloud.logging.tracelog.timeThreshold", 1);
	public static final int TRACE_LOGGING_SIMPLE_USE_TIME_OVER = ResourceUtils.getInt("mendmix-cloud.logging.tracelog.sampling.onUseTimeOverMillis", 1000);
	public static final int TRACE_LOGGING_SIMPLE_INTERVAL = ResourceUtils.getInt("mendmix-cloud.logging.tracelog.sampling.intervalSeconds", 180);
}
