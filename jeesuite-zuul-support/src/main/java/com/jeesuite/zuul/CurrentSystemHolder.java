package com.jeesuite.zuul;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.zuul.model.BizSystemModule;

/**
 * <br>
 * Class Name : CurrentSystemHolder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年12月3日
 */
public class CurrentSystemHolder {

	private static AtomicReference<Map<String, BizSystemModule>> routeModuleMappings = new AtomicReference<>();

	private static List<BizSystemModule> localModules;

	private static List<String> routeNames;

	public static BizSystemModule getModule(String route) {
		BizSystemModule module = routeModuleMappings.get().get(route);
		if (module == null) {
			module = routeModuleMappings.get().get(GlobalRuntimeContext.APPID);
		}
		return module;
	}

	public static Map<String, BizSystemModule> getRouteModuleMappings() {
		if (routeModuleMappings.get() == null) {
			load();
		}
		return new HashMap<>(routeModuleMappings.get());
	}

	public static List<String> getRouteNames() {
		if (routeModuleMappings.get() == null) {
			load();
		}
		return routeNames;
	}

	public static List<BizSystemModule> getModules() {
		if (routeModuleMappings.get() == null)
			return new ArrayList<>(0);
		return new ArrayList<>(routeModuleMappings.get().values());
	}

	private static synchronized void load() {
		if (localModules == null) {
			loadLocalRouteModules();
		}
		List<BizSystemModule> modules;
		try {
			modules = InstanceFactory.getInstance(SystemMgrApi.class).getSystemModules();
			if (!localModules.isEmpty()) {
				modules.addAll(localModules);
			}
		} catch (Exception e) {
			modules = localModules;
		}

		Map<String, BizSystemModule> _modules = new HashMap<>(modules.size());
		routeNames = new ArrayList<>(modules.size());
		for (BizSystemModule module : modules) {
			boolean isGateway = GlobalRuntimeContext.APPID.equalsIgnoreCase(module.getServiceId());
			if (!isGateway && StringUtils.isAnyBlank(module.getRouteName(), module.getServiceId())) {
				continue;
			}
			// 网关特殊处理
			if (isGateway) {
				module.setRouteName(GlobalRuntimeContext.APPID);
				_modules.put(GlobalRuntimeContext.APPID, module);
			} else {
				_modules.put(module.getRouteName(), module);
				routeNames.add(module.getRouteName());
			}
		}
		// 远程没配置网关模块
		if (!_modules.containsKey(GlobalRuntimeContext.APPID)) {
			BizSystemModule module = new BizSystemModule();
			module.setServiceId(GlobalRuntimeContext.APPID);
			module.setRouteName(GlobalRuntimeContext.APPID);
			module.setAnonymousUris(ResourceUtils.getProperty("application.anonymousUris"));
			//
			_modules.put(module.getRouteName(), module);
			localModules.add(module);
		}
		routeModuleMappings.set(_modules);

	}

	private static void loadLocalRouteModules() {
		localModules = new ArrayList<>();
		Properties properties = ResourceUtils.getAllProperties("zuul.routes.");
		Set<Entry<Object, Object>> entrySet = properties.entrySet();

		BizSystemModule module;
		for (Entry<Object, Object> entry : entrySet) {
			String key = entry.getKey().toString();
			if (!key.endsWith(".path"))
				continue;
			String prefix = key.replace(".path", "");
			module = new BizSystemModule();
			module.setRouteName(entry.getValue().toString().substring(1).replace("/**", ""));
			module.setServiceId(properties.getProperty(prefix + ".serviceId"));
			module.setAnonymousUris(properties.getProperty(prefix + ".anonymousUris"));
			localModules.add(module);
		}

	}
}
