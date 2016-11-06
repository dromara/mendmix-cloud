/**
 * 
 */
package com.jeesuite.monitor.web;

import com.jeesuite.monitor.model._MappingKit;
import com.jeesuite.monitor.web.controller.ConfigCenterController;
import com.jeesuite.monitor.web.controller.KafkaController;
import com.jeesuite.monitor.web.controller.SchedulerController;
import com.jeesuite.monitor.web.hanlder.GlobalsHandler;
import com.jfinal.config.Constants;
import com.jfinal.config.Handlers;
import com.jfinal.config.Interceptors;
import com.jfinal.config.JFinalConfig;
import com.jfinal.config.Plugins;
import com.jfinal.config.Routes;
import com.jfinal.log.Slf4jLogFactory;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.c3p0.C3p0Plugin;
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
		me.setLogFactory(new Slf4jLogFactory());
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
		C3p0Plugin c3p0Plugin = new C3p0Plugin(getProperty("db.url"), getProperty("db.username"), getProperty("db.password").trim());
		c3p0Plugin.setDriverClass(getProperty("db.jdbc.driver"))//
		          .setInitialPoolSize(10)//
		          .setMaxIdleTime(getPropertyToInt("maxIdleTime", 5))//
		          .setMaxPoolSize(getPropertyToInt("maxPoolSize", 150))//
		          .setMinPoolSize(getPropertyToInt("minPoolSize", 10));
		me.add(c3p0Plugin);
		
		// 配置ActiveRecord插件
		ActiveRecordPlugin arp = new ActiveRecordPlugin(c3p0Plugin);
		arp.setShowSql(getPropertyToBoolean("showSql", false));
		me.add(arp);
		//model -> 表映射
		_MappingKit.mapping(arp);
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
	public void configHandler(Handlers me) {
		me.add(new GlobalsHandler());
	}

	@Override
	public void afterJFinalStart() {}
	
	

}
