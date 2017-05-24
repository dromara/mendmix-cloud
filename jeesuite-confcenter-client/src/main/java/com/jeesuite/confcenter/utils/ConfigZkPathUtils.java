/**
 * 
 */
package com.jeesuite.confcenter.utils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月20日
 */
public class ConfigZkPathUtils {

	private static final String ZK_ROOT = "/confcenter/";
	
	public static String getConfigFilePath(String env,String app,String version,String fileName){
		String path = ZK_ROOT + env + "/" + app + "/" + version + "/" + fileName;
		return path;
	}
}
