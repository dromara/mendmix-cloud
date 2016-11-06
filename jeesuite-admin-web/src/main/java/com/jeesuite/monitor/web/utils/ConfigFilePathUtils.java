/**
 * 
 */
package com.jeesuite.monitor.web.utils;

import java.io.File;

import com.jeesuite.common.util.ResourceUtils;
import com.jfinal.kit.PathKit;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月3日
 */
public class ConfigFilePathUtils {

	private static String configBaseDir;

	public static String getRealPath() {
		String rootPath = PathKit.getWebRootPath();
		return rootPath.endsWith("/") ? rootPath.substring(0,rootPath.length() - 1) : rootPath;
	}
	

	public static String filePathToUrlPath(File file){
		String path = file.getAbsolutePath();
		path = path.replace(getRealPath(), "").replaceAll("\\\\", "/");
		return path;
	}

}
