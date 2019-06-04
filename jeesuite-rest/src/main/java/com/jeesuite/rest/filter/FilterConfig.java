/**
 * 
 */
package com.jeesuite.rest.filter;

import com.jeesuite.common.util.ResourceUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月25日
 */
public class FilterConfig {

	public static boolean corsEnabled(){
		return Boolean.parseBoolean(ResourceUtils.getProperty("cors.enabled", "false"));
	}
	
	public static String getCorsAllowOrgin(){
		return ResourceUtils.getProperty("cors.allow.origin", "*");
	}
	
	public static boolean reqRspLogEnabled(){
		return Boolean.parseBoolean(ResourceUtils.getProperty("reqres.log.enabled", "false"));
	}
	
	public static boolean apiDocEnabled(){
		return Boolean.parseBoolean(ResourceUtils.getProperty("apidoc.enabled", "true"));
	}
}
