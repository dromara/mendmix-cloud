/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.gateway;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.http.HostMappingHolder;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.gateway.api.SystemInfoApi;
import com.mendmix.gateway.model.BizSystem;
import com.mendmix.gateway.model.BizSystemModule;
import com.mendmix.gateway.model.BizSystemPortal;
import com.mendmix.gateway.task.ModuleApiRefreshTask;
import com.mendmix.spring.DataChangeEvent;
import com.mendmix.spring.InstanceFactory;

/**
 * <br>
 * Class Name : CurrentSystemHolder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年12月3日
 */
public class CurrentSystemHolder {

	private static Logger log = LoggerFactory.getLogger("com.mendmix.gateway");

	private static boolean multiSystemMode = ResourceUtils.getBoolean("application.proxy.multi-system.enabled", false);

	private static BizSystem mainSystem; // 主系统
	private static List<BizSystem> systems = new ArrayList<>();

	private static Map<String, BizSystem> domainSystemMappings = new HashMap<>();
	private static Map<String, BizSystemModule> routeModuleMappings = new HashMap<>();

	private static List<BizSystemModule> localModules;

	private static List<String> routeNames = new ArrayList<>();

	public static BizSystem getSystem() {
		if (mainSystem == null) {
			load();
		}
		return mainSystem;
	}

	public static List<BizSystem> getSystems() {
		if (mainSystem == null) {
			load();
		}
		return systems;
	}

	public static BizSystem getSystem(String domain, String routeName) {
		if (mainSystem == null) {
			load();
		}

		if (!multiSystemMode) {
			return mainSystem;
		}

		if (domain != null && domainSystemMappings.containsKey(domain)) {
			return domainSystemMappings.get(domain);
		}

		for (BizSystem system : systems) {
			for (BizSystemModule module : system.getModules()) {
				if (StringUtils.equals(module.getRouteName(), routeName)) {
					return system;
				}
			}
		}

		return mainSystem;
	}

	public static BizSystemPortal getSystemPortal(String domain) {
		if (StringUtils.isBlank(domain))
			return null;
		if (mainSystem == null) {
			load();
		}

		if (domainSystemMappings.containsKey(domain)) {
			List<BizSystemPortal> portals = domainSystemMappings.get(domain).getPortals();
			for (BizSystemPortal portal : portals) {
				if (StringUtils.equals(portal.getDomain(), domain)) {
					return portal;
				}
			}
			log.warn("MENDMIX-TRACE-LOGGGING-->> can't match BizSystemPortal -> domain:{}", domain);

		}
		return null;
	}

	public static BizSystemModule getModule(String route) {
		if (mainSystem == null) {
			load();
		}
		BizSystemModule module = routeModuleMappings.get(route);
		if (module == null) {
			module = routeModuleMappings.get(GlobalRuntimeContext.APPID);
		}
		return module;
	}

	public static Map<String, BizSystemModule> getRouteModuleMappings() {
		if (mainSystem == null) {
			load();
		}
		return Collections.unmodifiableMap(routeModuleMappings);
	}

	public static List<String> getRouteNames() {
		if (mainSystem == null) {
			load();
		}
		return routeNames;
	}

	public static List<BizSystemModule> getModules() {
		if (mainSystem == null) {
			load();
		}
		return new ArrayList<>(routeModuleMappings.values());
	}

	public static void reset() {
		if (localModules != null)
			localModules.clear();
		localModules = null;
		mainSystem = null;
		systems.clear();
		routeModuleMappings.clear();
		domainSystemMappings.clear();
		routeNames.clear();
	}

	public static synchronized void load() {
		if (mainSystem != null)
			return;
		loadLocalRouteModules();
		loadRemoteSystems();
		//
		for (BizSystemModule module : routeModuleMappings.values()) {
			module.format();
			ModuleApiRefreshTask.initModuleApiInfos(module);
			//
			if (!module.isGateway() && !HostMappingHolder.containsProxyUrlMapping(module.getServiceId())) {
				if (module.getProxyUri().startsWith("http") || module.getStripPrefix() != 2) {
					HostMappingHolder.addProxyUrlMapping(module.getServiceId(), module.getServiceBaseUrl());
					log.info("MENDMIX-TRACE-LOGGGING-->> add host mapping : {} = {}", module.getServiceId(),
							HostMappingHolder.getProxyUrlMapping(module.getServiceId()));
				} 
			}
		}
		InstanceFactory.getContext().publishEvent(new DataChangeEvent("moduleApis", new Object()));

		StringBuilder logBuilder = new StringBuilder(
				"MENDMIX-TRACE-LOGGGING-->> \n============load systems begin================");
		logBuilder.append("\nmainSystem:\n -").append(mainSystem.toString());
		if (multiSystemMode) {
			logBuilder.append("\nmountSystem:");
			for (BizSystem system : systems) {
				if (StringUtils.equals(system.getId(), mainSystem.getId()))
					continue;
				logBuilder.append("\n -").append(system.toString());
			}
		}
		logBuilder.append("\n============load systems end================\n");
		log.info(logBuilder.toString());
	}

	private static void loadRemoteSystems() {
		SystemInfoApi apiInstance = InstanceFactory.getInstance(SystemInfoApi.class);
		if (apiInstance != null) {
			mainSystem = apiInstance.getSystemMetadata(GlobalRuntimeContext.SYSTEM_ID);
		}
		if(mainSystem == null) {
			mainSystem = new BizSystem();
			mainSystem.setId(ResourceUtils.getProperty("default.allocation.system.id", "0"));
			log.warn("MENDMIX-TRACE-LOGGGING-->> system [{}] not found!!!!!!", GlobalRuntimeContext.SYSTEM_ID);
		}
		// 全局模块
		if (apiInstance != null && ResourceUtils.getBoolean("application.global-route.enabled", true)) {
			List<BizSystemModule> globalModules = apiInstance.getGlobalModules();
			if(globalModules != null) {
				log.info("MENDMIX-TRACE-LOGGGING-->> globalModules size:{}", globalModules.size());
				for (BizSystemModule module : globalModules) {
					if (mainSystem.getModules().contains(module))
						continue;
					module.setGlobal(true);
					module.setSystemId(mainSystem.getId());
					mainSystem.getModules().add(module);
				}
			}
		}

		buildMappingCacheData(mainSystem, true);
		// 多系统模式
		if (apiInstance != null && multiSystemMode) {
			List<String> systemCodes = null;
			if (ResourceUtils.containsProperty("application.proxy.multi-system.ids")) {
				systemCodes = ResourceUtils.getList("application.proxy.multi-system.ids");

			} else {
				systemCodes = apiInstance.getSubSystemIdentifiers(mainSystem.getId());
			}
			//
			if(systemCodes != null) {
				BizSystem system;
				for (String systemCode : systemCodes) {
					system = apiInstance.getSystemMetadata(systemCode);
					//
					buildMappingCacheData(system, false);
				}
			}
		}
	}

	private static void buildMappingCacheData(BizSystem system, boolean isMainSystem) {
		systems.add(system);
		//
		BizSystemModule gatewayModule = null; // 网关模块
		Iterator<BizSystemModule> iterator = system.getModules().iterator();
		while(iterator.hasNext()) {
			BizSystemModule module = iterator.next();
			if(localModules.contains(module)) {
				BizSystemModule matchLocalModule = localModules.stream().filter(o -> o.equals(module)).findFirst().orElse(null);
				matchLocalModule.setId(module.getId());
				iterator.remove();
				log.info("MENDMIX-TRACE-LOGGGING-->> merge local module[{}-{}]!!!!!",module.getRouteName(),module.getServiceId());
				continue;
			}
			if (StringUtils.isAnyBlank(module.getRouteName(), module.getProxyUri())) {
				log.info("MENDMIX-TRACE-LOGGGING-->> ignore error module[{}-{}]!!!!!", module.getRouteName(),
						module.getServiceId());
				continue;
			}
			//
			if (isMainSystem && GlobalRuntimeContext.APPID.equalsIgnoreCase(module.getServiceId())) {
				gatewayModule = module;
			} else {
				routeModuleMappings.put(module.getRouteName(), module);
				routeNames.add(module.getRouteName());
			}
		}
		//
		for (BizSystemPortal portal : system.getPortals()) {
			if (StringUtils.isNotBlank(portal.getIndexPath())) {
				domainSystemMappings.put(portal.getDomain(), system);
				log.info("MENDMIX-TRACE-LOGGGING-->> add domainSystemMappings: {} = {}", portal.getDomain(),
						system.getId());
			}
		}
		//
		if (isMainSystem) {
			for (BizSystemModule module : localModules) {
				routeModuleMappings.put(module.getRouteName(), module);
				routeNames.add(module.getRouteName());
				system.getModules().add(module);
			}

			// 网关本身
			if (gatewayModule == null) {
				gatewayModule = new BizSystemModule();
				gatewayModule.setServiceId(GlobalRuntimeContext.APPID);
				system.getModules().add(gatewayModule);
			}
			gatewayModule.setRouteName(GlobalRuntimeContext.APPID);
			gatewayModule.setStripPrefix(0);
			gatewayModule.setRouteName(GlobalRuntimeContext.APPID);
			routeModuleMappings.put(gatewayModule.getRouteName(), gatewayModule);

		}
	}

	private static void loadLocalRouteModules() {
		if (localModules != null)
			return;
		localModules = new ArrayList<>();
		Properties properties = ResourceUtils.getAllProperties("spring.cloud.gateway.routes");
		Set<Entry<Object, Object>> entrySet = properties.entrySet();

		BizSystemModule module;
		String prefix;
		for (Entry<Object, Object> entry : entrySet) {
			if (entry.getKey().toString().endsWith(".id")) {
				prefix = entry.getKey().toString().replace(".id", "");
				module = new BizSystemModule();
				module.setServiceId(entry.getValue().toString());
				module.setProxyUri(properties.getProperty(prefix + ".uri"));
				PredicateDefinition pathPredicate = new PredicateDefinition(
						properties.getProperty(prefix + ".predicates[0]"));
				String pathPredicateValue = pathPredicate.getArgs().get("_genkey_0");
				if (!pathPredicateValue.startsWith(GatewayConfigs.PATH_PREFIX)) {
					log.warn("ZVOS-FRAMEWORK-STARTUP-LOGGGING-->> route_format_error ->routeId:{},pathPredicateValue",
							entry.getKey(), pathPredicateValue);
					throw new IllegalArgumentException("route path must startWith:" + GatewayConfigs.PATH_PREFIX);
				}
				//
				String routeName = pathPredicateValue.substring(GatewayConfigs.PATH_PREFIX.length() + 1);
				routeName = routeName.substring(0,routeName.lastIndexOf("/"));
				module.setRouteName(routeName);
				//
				FilterDefinition filterDefinition = new FilterDefinition(
						properties.getProperty(prefix + ".filters[0]"));
				String stripPrefix = filterDefinition.getArgs().get("_genkey_0");
				module.setStripPrefix(Integer.parseInt(stripPrefix));
				localModules.add(module);
			}
		}

	}

}
