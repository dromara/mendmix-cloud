/**
 * 
 */
package com.jeesuite.monitor.web.controller;

import java.io.File;

import com.jeesuite.monitor.web.utils.ConfigFilePathUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月3日
 */
public class ConfigCenterController extends BaseController{
	
	public void uplaod(){
		
	}
	
    public void check(){
		
	}
	
	public void download(){
		String app = getPara(0);
		String env = getPara("env");
		String version = getPara("ver","0.0.0");
		String fileName = getPara("file");
		
		File file = ConfigFilePathUtils.getConfigFile(app, env, version,fileName);
		if(file == null){
			ajaxError("file_not_found");
		}else{
			renderFile(file);
		}
		
	}
}
