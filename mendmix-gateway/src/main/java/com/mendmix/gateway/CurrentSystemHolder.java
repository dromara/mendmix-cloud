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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.http.HostMappingHolder;
import com.mendmix.common.http.HttpRequestEntity;
import com.mendmix.common.model.ApiInfo;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.gateway.api.SystemMgtApi;
import com.mendmix.gateway.model.BizSystem;
import com.mendmix.gateway.model.BizSystemModule;
import com.mendmix.gateway.model.BizSystemPortal;
import com.mendmix.spring.DataChangeEvent;
import com.mendmix.spring.InstanceFactory;
import com.mendmix.springweb.exporter.AppMetadataHolder;
import com.mendmix.springweb.model.AppMetadata;

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

	private static Map<String, Map<String, ApiInfo>> moduleApiInfos = new HashMap<>();

	private static int fetchApiMetaRetries = 0;

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
		}

		StringBuilder logBuilder = new StringBuilder("MENDMIX-TRACE-LOGGGING-->> \n============load systems begin================");
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

		// 查询api信息
		if (fetchApiMetaRetries == 0) {
			new Thread(() -> {
				while (routeModuleMappings.values().size() > moduleApiInfos.size() && fetchApiMetaRetries < 360) {
					for (BizSystemModule module : routeModuleMappings.values()) {
						if (moduleApiInfos.containsKey(module.getServiceId()))
							continue;
						initModuleApiInfos(module);
					}
					fetchApiMetaRetries++;
					try {
						Thread.sleep(10000);
					} catch (Exception e) {
					}
				}
			}).start();
		}
	}

	private static void loadRemoteSystems() {
		SystemMgtApi apiInstance = InstanceFactory.getInstance(SystemMgtApi.class);
		if (apiInstance != null) {
			mainSystem = apiInstance.getSystemMetadata(GlobalRuntimeContext.SYSTEM_ID);
		} else {
			mainSystem = new BizSystem();
			mainSystem.setId(ResourceUtils.getProperty("default.allocation.system.id", "0"));
			log.warn("MENDMIX-TRACE-LOGGGING-->> system [{}] not found!!!!!!", GlobalRuntimeContext.SYSTEM_ID);
		}

		// 全局模块
		if (apiInstance != null && ResourceUtils.getBoolean("application.base-route.enabled", true)) {
			List<BizSystemModule> globalModules = apiInstance.getGlobalModules();
			log.info("MENDMIX-TRACE-LOGGGING-->> globalModules size:{}", globalModules.size());
			for (BizSystemModule module : globalModules) {
				if (mainSystem.getModules().contains(module))
					continue;
				module.setGlobal(true);
				module.setSystemId(mainSystem.getId());
				if (!HostMappingHolder.containsProxyUrlMapping(module.getServiceId())) {
					if (module.getProxyUri().endsWith("/" + module.getRouteName())) {
						HostMappingHolder.addProxyUrlMapping(module.getServiceId(), module.getProxyUri());
					} else {
						String mappingValue = StringUtils.split(module.getProxyUri(), "/")[1];
						HostMappingHolder.addProxyUrlMapping(module.getServiceId(), mappingValue);
					}
					log.info("MENDMIX-TRACE-LOGGGING-->> add host mapping : {} = {}", module.getServiceId(),
							HostMappingHolder.getProxyUrlMapping(module.getServiceId()));
				}
				mainSystem.getModules().add(module);
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
			BizSystem system;
			for (String systemCode : systemCodes) {
				system = apiInstance.getSystemMetadata(systemCode);
				//
				buildMappingCacheData(system, false);
			}
		}
	}

	private static void buildMappingCacheData(BizSystem system, boolean isMainSystem) {
		systems.add(system);
		//
		BizSystemModule gatewayModule = null; // 网关模块
		List<BizSystemModule> modules = system.getModules();
		for (BizSystemModule module : modules) {
			if (localModules.contains(module)) {
				log.info("MENDMIX-TRACE-LOGGGING-->> ignore reduplicate module[{}-{}]!!!!!",
						module.getRouteName(), module.getServiceId());
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
			}

			// 网关本身
			if (gatewayModule == null) {
				gatewayModule = new BizSystemModule();
				gatewayModule.setServiceId(GlobalRuntimeContext.APPID);
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

		List<RouteDefinition> defaultRouteDefs = InstanceFactory.getInstance(GatewayProperties.class).getRoutes();

		Properties properties = ResourceUtils.getAllProperties("spring.cloud.gateway.routes");
		Set<Entry<Object, Object>> entrySet = properties.entrySet();

		BizSystemModule module;
		String prefix;
		for (Entry<Object, Object> entry : entrySet) {
			if (entry.getKey().toString().endsWith(".id")) {
				prefix = entry.getKey().toString().replace(".id", "");
				module = new BizSystemModule();
				module.setDefaultRoute(true);
				module.setServiceId(entry.getValue().toString());
				module.setProxyUri(properties.getProperty(prefix + ".uri"));
				//
				updateModuleRouteInfos(module, defaultRouteDefs);
				localModules.add(module);
				HostMappingHolder.addProxyUrlMapping(module.getServiceId(), module.getProxyUri());
			}
		}

	}

	/**
	 * @param module
	 * @param definition
	 */
	private static void updateModuleRouteInfos(BizSystemModule module, List<RouteDefinition> defaultRouteDefs) {
		RouteDefinition routeDef = defaultRouteDefs.stream()
				.filter(def -> StringUtils.equalsIgnoreCase(module.getServiceId(), def.getId())).findFirst()
				.orElse(null);
		if (routeDef == null)
			return;
		FilterDefinition stripPrefixDef = routeDef.getFilters().stream().filter(p -> "StripPrefix".equals(p.getName()))
				.findFirst().orElse(null);
		int stripPrefix = 0;
		if (stripPrefixDef != null) {
			stripPrefix = Integer.parseInt(stripPrefixDef.getArgs().get("_genkey_0"));
		}
		module.setStripPrefix(stripPrefix);
		PredicateDefinition pathDef = routeDef.getPredicates().stream().filter(p -> "Path".equals(p.getName()))
				.findFirst().orElse(null);
		if (pathDef != null) {
			String pathPattern = pathDef.getArgs().get("_genkey_0");
			if (!pathPattern.startsWith(GatewayConfigs.PATH_PREFIX)) {
				throw new MendmixBaseException("route path must startWith:" + GatewayConfigs.PATH_PREFIX);
			}
			String[] parts = StringUtils.split(pathPattern, "/");
			module.setRouteName(parts[1]);
		}
	}

	private static void initModuleApiInfos(BizSystemModule module) {
		try {
			String url;
			AppMetadata appMetadata;
			if (GlobalRuntimeContext.APPID.equals(module.getRouteName())) {
				appMetadata = AppMetadataHolder.getMetadata();
			} else {
				url = module.getMetadataUri();
				appMetadata = HttpRequestEntity.get(url).backendInternalCall().execute().toObject(AppMetadata.class);
			}
			for (ApiInfo api : appMetadata.getApis()) {
				module.addApiInfo(api);
			}
			moduleApiInfos.put(module.getServiceId(), module.getApiInfos());
			InstanceFactory.getContext().publishEvent(new DataChangeEvent("moduleApis", new Object()));
			log.info("MENDMIX-TRACE-LOGGGING-->> initModuleApiInfos success -> serviceId:{},nums:{}", module.getServiceId(),
					module.getApiInfos().size());
		} catch (Exception e) {
			boolean ignore = e instanceof ClassCastException;
			if (!ignore && e instanceof MendmixBaseException) {
				MendmixBaseException ex = (MendmixBaseException) e;
				ignore = ex.getCode() == 404 || ex.getCode() == 401 || ex.getCode() == 403;
			}
			if (ignore) {
				module.setApiInfos(new HashMap<>(0));
				moduleApiInfos.put(module.getServiceId(), module.getApiInfos());
			} else if (fetchApiMetaRetries <= 1) {
				log.error("MENDMIX-TRACE-LOGGGING-->> initModuleApiInfos error -> serviceId:[" + module.getServiceId() + "]", e);
			}
		}
	}

}
