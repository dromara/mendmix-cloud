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


	public static String getConfigBaseDir() {
		if(configBaseDir != null)return configBaseDir;
		configBaseDir = ResourceUtils.get("conf.file.dir");
		if(configBaseDir == null || !new File(configBaseDir).exists()){
			throw new RuntimeException("Property[conf.file.dir] is not define");
		}
		configBaseDir = configBaseDir.endsWith("/") ? configBaseDir : configBaseDir + "/";
		return configBaseDir;
	}
	
	public static File getConfigFile(String env,String app,String version,String fileName){
		String path = getConfigBaseDir() + String.format("%s/%s/%s/%s", app,env,version,fileName);
		File file = new File(path);
		return file.exists() ? file : null;
	}

}
