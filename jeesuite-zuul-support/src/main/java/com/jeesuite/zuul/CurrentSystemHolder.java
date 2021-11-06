package com.jeesuite.zuul;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.zuul.model.BizSystemModule;

/**
 * <br>
 * Class Name   : CurrentSystemHolder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年12月3日
 */
public class CurrentSystemHolder {


	private static AtomicReference<Map<String, BizSystemModule>> routeModuleMappings = new AtomicReference<>();

	private static BizSystemModule defaultModule;
	
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
		List<BizSystemModule> modules;
		try {
			modules = InstanceFactory.getInstance(SystemMgrApi.class).getSystemModules();
		} catch (Exception e) {
			modules = new ArrayList<>(0);
		}
		routeModuleMappings.set(modules.stream().collect(Collectors.toMap(BizSystemModule::getRouteName, Function.identity())));
		//
		if(defaultModule == null) {
			defaultModule = new BizSystemModule();
			defaultModule.setRouteName(GlobalRuntimeContext.APPID);
			defaultModule.setAnonymousUris(ResourceUtils.getProperty("application.global.anonymousUris"));
		}
	}
}
