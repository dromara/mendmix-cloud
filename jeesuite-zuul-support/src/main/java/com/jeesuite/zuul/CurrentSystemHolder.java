/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.zuul;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.zuul.model.BizSystem;
import com.jeesuite.zuul.model.BizSystemModule;

/**
 * <br>
 * Class Name   : SystemModuleHolder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年12月3日
 */
public class CurrentSystemHolder {


	private static BizSystem system;
	private static AtomicReference<Map<String, BizSystemModule>> routeModuleMappings = new AtomicReference<>();

	private static BizSystemModule defaultModule;
	

	public static BizSystem getSystem() {
		return system;
	}

	public static BizSystemModule getModule(String route){
		BizSystemModule module = routeModuleMappings.get().get(route);
		return module != null ? module : defaultModule;
	}
	
	public static Map<String, BizSystemModule>  getRouteModuleMappings(){
		if(routeModuleMappings.get() == null) {
			load();
		}
		return new HashMap<>(routeModuleMappings.get());
	}
	
	public static List<BizSystemModule> getModules(){
		return new ArrayList<>(routeModuleMappings.get().values());
	}
	
	private static synchronized void load(){
		system = ServiceInstances.systemMgrApi().getSystemMetadata();
		Map<String, BizSystemModule> _modules = new HashMap<>(system.getModules().size());
		for (BizSystemModule module : system.getModules()) {
			_modules.put(module.getRouteName(), module);
		}
		routeModuleMappings.set(_modules);
		//
		if(defaultModule == null) {
			defaultModule = new BizSystemModule();
			defaultModule.setRouteName(system.getCode());
			defaultModule.setAnonymousUris(ResourceUtils.getProperty("global.anonymousUris"));
		}
	}
}
