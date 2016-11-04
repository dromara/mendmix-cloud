/**
 * 
 */
package com.jeesuite.monitor.web;

import com.jeesuite.monitor.web.controller.ConfigCenterController;
import com.jeesuite.monitor.web.controller.KafkaController;
import com.jeesuite.monitor.web.controller.SchedulerController;
import com.jeesuite.monitor.web.utils.ConfigFilePathUtils;
import com.jfinal.config.Constants;
import com.jfinal.config.Handlers;
import com.jfinal.config.Interceptors;
import com.jfinal.config.JFinalConfig;
import com.jfinal.config.Plugins;
import com.jfinal.config.Routes;
import com.jfinal.render.ViewType;

public class WebAppConfig extends JFinalConfig {
	
	@Override
	public void configConstant(Constants me) {
		loadPropertyFile("config.properties");
		me.setDevMode(getPropertyToBoolean("devMode", true));
		me.setBaseViewPath("/WEB-INF/template");
		me.setViewType(ViewType.FREE_MARKER);
		me.setError404View("/404.html");
		me.setError500View("/500.html");	
		me.setFreeMarkerViewExtension(".html");
	}
	
	/**

	 * 配置路由

	 */
	@Override
	public void configRoute(Routes me) {
		me.add("/kafka", KafkaController.class,"kafka");	
		me.add("/scheduler", SchedulerController.class,"scheduler");
		me.add("/confcenter", ConfigCenterController.class,"confcenter");
	}
	
	/**

	 * 配置插件

	 */
	@Override
	public void configPlugin(Plugins me) {

	}
	
	/**

	 * 配置全局拦截器

	 */
	@Override
	public void configInterceptor(Interceptors me) {}
	
	/**

	 * 配置处理器

	 */
	@Override
	public void configHandler(Handlers me) {}

	@Override
	public void afterJFinalStart() {
		ConfigFilePathUtils.getConfigBaseDir();
	}
	
	

}
