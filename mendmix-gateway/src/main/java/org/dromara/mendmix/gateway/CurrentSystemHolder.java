/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.gateway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.http.HttpRequestEntity;
import org.dromara.mendmix.common.model.KeyValuePair;
import org.dromara.mendmix.common.util.ExceptionFormatUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.common.util.WebUtils;
import org.dromara.mendmix.gateway.api.SystemInfoApi;
import org.dromara.mendmix.gateway.model.BizSystem;
import org.dromara.mendmix.gateway.model.BizSystemModule;
import org.dromara.mendmix.gateway.model.BizSystemPortal;
import org.dromara.mendmix.gateway.model.CompositeModule;
import org.dromara.mendmix.gateway.model.SubSystem;
import org.dromara.mendmix.gateway.task.ModuleApiRefreshTask;
import org.dromara.mendmix.spring.InstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;

/**
 * <br>
 * Class Name : CurrentSystemHolder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年12月3日
 */
public class CurrentSystemHolder {

	private static Logger log = LoggerFactory.getLogger("org.dromara.mendmix");

	private static boolean remoteRouteEnabled = ResourceUtils.getBoolean("mendmix-cloud.remote-route.enabled",true);
	private static boolean baseRouteEnabled = ResourceUtils.getBoolean("mendmix-cloud.base-route.enabled",true);
	private static boolean multiSystemMode = ResourceUtils.getBoolean("mendmix-cloud.proxy.multi-system.enabled",false);
	
	private static AtomicReference<LocalCacheHub> localCacheHubRef = new AtomicReference<>();

	private static Map<String, String> systemIdOrCodeRelMapping = new HashMap<>();
	
	public static BizSystem getSystem() {
		return localCacheHubRef.get().mainSystem;
	}
	
	public static List<BizSystem> getSystems() {
		return localCacheHubRef.get().systems;
	}
	
	public static String validateSystemId(String systemIdOrCode) {
		return validateSystemId(systemIdOrCode, false);
	}
	
	public static String validateSystemId(String systemIdOrCode,boolean subSystem) {
		if(systemIdOrCodeRelMapping.containsKey(systemIdOrCode)) {
			return getCachingSystemId(systemIdOrCode);
		}
		String systemId = null;
		synchronized (systemIdOrCodeRelMapping) {
			if(systemIdOrCodeRelMapping.containsKey(systemIdOrCode)) {
				return getCachingSystemId(systemIdOrCode);
			}
			BizSystem system = getBizSystem(systemIdOrCode);
			if(system == null) {
				
			}
			if(system == null) {
				systemIdOrCodeRelMapping.put(systemIdOrCode, null);
			}else {
				systemId = system.getId();
				systemIdOrCodeRelMapping.put(system.getId(), system.getCode());
				systemIdOrCodeRelMapping.put(system.getCode(), system.getId());
			}
		}
		return systemId;
	}
	
	public static String getCachingSystemKey(String systemId) {
		if(!systemIdOrCodeRelMapping.containsKey(systemId)) {
			validateSystemId(systemId);
		}
		return systemIdOrCodeRelMapping.get(systemId);
	}
	
	public static String getCachingSystemId(String systemIdOrCode) {
		String value = systemIdOrCodeRelMapping.get(systemIdOrCode);
		if(value != null) {
			if(StringUtils.isNumeric(systemIdOrCode)) {
				return systemIdOrCode;
			}
			return value;
		}
		return null;
	}

    public static BizSystem getBizSystem(String idOrCode) {
    	return getSystems().stream().filter(o -> {
    		return idOrCode.equals(o.getId()) || idOrCode.equals(o.getCode());
    	}).findFirst().orElse(null);
    }

	public static BizSystem getSystem(String domain,String routeName) {
		if(!multiSystemMode) {
			return localCacheHubRef.get().mainSystem;
		}
		
		if(domain != null && localCacheHubRef.get().domainSystemMappings.containsKey(domain)) {
			return localCacheHubRef.get().domainSystemMappings.get(domain);
		}
		
		for (BizSystem system : localCacheHubRef.get().systems) {
			for (BizSystemModule module : system.getModules()) {
				if(module.isGlobal() || module.isGateway())continue;
				if(module.getResolveRouteNames().contains(routeName)) {
					return system;
				}
			}
		}
		return null;
	}
	
	public static BizSystemPortal getSystemPortal(String domain) {
		if(StringUtils.isBlank(domain))return null;
		if(localCacheHubRef.get().domainSystemMappings.containsKey(domain)) {
			List<BizSystemPortal> portals = localCacheHubRef.get().domainSystemMappings.get(domain).getPortals();
			for (BizSystemPortal portal : portals) {
				if(StringUtils.equals(portal.getDomain(), domain)) {
					return portal;
				}
			}
			log.warn(">> can't match BizSystemPortal -> domain:{}",domain);
			
		}
		return null;
	}
	

	public static BizSystemModule getModule(String route){
		BizSystemModule module = localCacheHubRef.get().routeModuleRelMappings.get(route);
		if(module == null) {
			module = localCacheHubRef.get().routeModuleRelMappings.get(GlobalContext.APPID);
		}
		return module;
	}
	
	public static BizSystemModule getModuleByServiceId(String serviceId){
		return getModules().stream().filter(
				m -> StringUtils.equalsIgnoreCase(serviceId, m.getServiceId())
		).findFirst().orElse(null);
	}
	
	
	public static List<String>  getRouteNames(){
		return localCacheHubRef.get().routeNames;
	}

	public static List<BizSystemModule> getModules(){
		return localCacheHubRef.get().modules;
	}
	
	public static  KeyValuePair getOpenClientIdSecret(String systemId) {
		if(systemId == null)return null;
		return localCacheHubRef.get().openClientMappings.get(systemId);
	}
	
	public static List<BizSystemModule> getAndLoadModules(){
		if(localCacheHubRef.get() == null) {
			LocalCacheHub newLocalCacheHub = new LocalCacheHub();
			load(newLocalCacheHub);
			localCacheHubRef.set(newLocalCacheHub);
		}
		return localCacheHubRef.get().modules;
	}
	
	public static void reload() {
		log.info(">> SystemMetadata reload BEGIN...");
		LocalCacheHub localCacheHub = new LocalCacheHub();
		load(localCacheHub);
		LocalCacheHub originCache = localCacheHubRef.get();
		localCacheHubRef.set(localCacheHub);
		if(originCache != null) {
			originCache.destory();
		}
		//
		initModuleApiInfos(false);
		log.info(">> SystemMetadata reload END!!!");
	}
	
	private static synchronized void load(LocalCacheHub localCacheHub){
		if(localCacheHub.mainSystem != null)return;
		String proValue = ResourceUtils.getProperty("mendmix-cloud.proxy.multi-system.ignoreSubRoutes");
		if(StringUtils.isNotBlank(proValue)) {
			//mall:order,common;
			String[] parts = StringUtils.split(proValue, ";");
			String[] subParts;
			for (String part : parts) {
				if(StringUtils.isBlank(part))continue;
				subParts = StringUtils.split(part, ":");
				localCacheHub.ignoreSubRouteMappings.put(subParts[0], Arrays.asList(StringUtils.split(subParts[1], ",")));
			}
		}
		loadAllModules(localCacheHub);
		//覆盖路由
		String proxyUri;
		for (BizSystemModule module : localCacheHub.modules) {
			proxyUri = ResourceUtils.getProperty(module.getServiceId() + ".route.proxyUri");
			if(StringUtils.isNotBlank(proxyUri)) {
				module.setProxyUri(proxyUri);
				log.info(" 覆盖服务[{}]路由:{}",module.getServiceId(),proxyUri);
			}
			module.format();
			// 通过子系统网关代理模式
			if (!module.isSubGateway() && !module.isGateway()) {
				module.setSubGateway(module.getStripPrefix() == 0);
			}
		}
		
		StringBuilder logBuilder = new StringBuilder("> \n============load systems begin================");
		logBuilder.append("\nmainSystem:\n -").append(localCacheHub.mainSystem.toString());
		if(multiSystemMode) {
			logBuilder.append("\nmountSystem:");
			for (BizSystem system : localCacheHub.systems) {
				if(StringUtils.equals(system.getId(), localCacheHub.mainSystem.getId()))continue;
				logBuilder.append("\n -").append(system.toString());
			}
		}
		logBuilder.append("\n============load systems end================\n");
		log.info(logBuilder.toString());
	}
	
	public static void initModuleApiInfos(boolean forceUpdate) {
		log.info(">>initModuleApiInfos BEGIN...");
		List<BizSystem> systems = CurrentSystemHolder.getSystems();
		int mouleNums = 0;
		for (BizSystem system : systems) {
			mouleNums+=system.getModules().size();
		}
		int poolSize = mouleNums < 10 ? mouleNums : 10;
		ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
		CountDownLatch counter = new CountDownLatch(mouleNums);
		List<BizSystemModule> errorModules = new ArrayList<>();
		try {
			for (BizSystem system : systems) {
				for (BizSystemModule module : system.getModules()) {
					executorService.execute(new Runnable() {
						@Override
						public void run() {
							boolean result = ModuleApiRefreshTask.initModuleApiInfos(module);
							if(!result) {
								errorModules.add(module);
								log.info(">> 模块[{},{}] 初始化apis失败，加入重试队列",module.getServiceId(),module.getMetadataUri());
							}
							counter.countDown();
						}
					});
				}
			}
			counter.await();
			executorService.shutdown();
			log.info(">>initModuleApiInfos END!!!");
		} catch (Exception e) {
			executorService.shutdown();
		}
		//
		if(errorModules.size() > 0) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < 12; i++) {
						try {Thread.sleep(5000);} catch (Exception e) {}
						Iterator<BizSystemModule> iterator = errorModules.iterator();
						while(iterator.hasNext()) {
							BizSystemModule module = iterator.next();
							if(ModuleApiRefreshTask.initModuleApiInfos(module)) {
								iterator.remove();
								log.info(">> 模块[{},{}] 重试初始化apis完成！！！",module.getServiceId(),module.getMetadataUri());
							}
						}
					}
				}
			}).start();
		}
	}
	
	private static void loadAllModules(LocalCacheHub localCacheHub) {
		log.info(" loadRemoteSystems BEGIN...");
		SystemInfoApi apiInstance = InstanceFactory.getInstance(SystemInfoApi.class);
		if(apiInstance != null) {
			log.info("> load mainSystem [{}] BEGIN...",GlobalContext.SYSTEM_KEY);
			localCacheHub.mainSystem = apiInstance.getSystemMetadata(GlobalContext.SYSTEM_KEY);
			Validate.notNull(localCacheHub.mainSystem, "系统【"+GlobalContext.SYSTEM_KEY+"】不存在");
			log.info("> load mainSystem [{}] END.",GlobalContext.SYSTEM_KEY);
		}else {
			localCacheHub.mainSystem = new BizSystem();
			localCacheHub.mainSystem.setId(ResourceUtils.getProperty("default.allocation.system.id", "0"));
			log.warn("\n<waring>!!!!\n<waring>!!!!\n system [{}] not found!!!!!!",GlobalContext.SYSTEM_KEY);
		}
		//本地模块
		loadLocalRouteModules(localCacheHub);
		//全局模块
		if(apiInstance != null && baseRouteEnabled) {
			List<String> baseRouteNames = ResourceUtils.getList("mendmix-cloud.base-route.names");
			List<BizSystemModule> globalModules = apiInstance.getGlobalModules();
			log.info("> globalModules size:{}",globalModules.size());
        	for (BizSystemModule module : globalModules) {
        		boolean existRoute = localCacheHub.mainSystem.getModules().stream().anyMatch(o -> o.getResolveRouteNames() != null && o.getResolveRouteNames().contains(module.getFirstRouteName()));
        		if(existRoute) {
        			log.info("> 忽略应用已定义重复的 globalModules {}:{}",module.getServiceId(),module.getFirstRouteName());
        			continue;
        		}
        		if(!baseRouteNames.isEmpty() && !baseRouteNames.contains(module.getFirstRouteName())) {
        			log.info("> ignore globalModules {}:{}",module.getServiceId(),module.getFirstRouteName());
        			continue;
        		}
        		module.setGlobal(true);
        		module.setSystemId(localCacheHub.mainSystem.getId());
        		localCacheHub.mainSystem.getModules().add(module);
			}
		}
    	
		localCacheHub.systems.add(localCacheHub.mainSystem);
		buildMappingCacheData(localCacheHub,localCacheHub.mainSystem, true);
		//多系统模式
		if(apiInstance != null && multiSystemMode) {
			List<SubSystem> subSystems = null;
			if(ResourceUtils.containsProperty("mendmix-cloud.proxy.multi-system.ids")) {
				List<String> systemCodes = ResourceUtils.getList("mendmix-cloud.proxy.multi-system.ids");
				subSystems = new ArrayList<>(systemCodes.size());
				SubSystem subSystem;
				for (String systemCode : systemCodes) {
					subSystem = new SubSystem();
					subSystem.setCode(systemCode);
					subSystem.setRouteMode(ResourceUtils.getBoolean("mendmix-cloud.proxy.multi-system.routeMode",true));
					subSystems.add(subSystem);
				}
			}else {
				try {					
					subSystems = apiInstance.getSubSystems(localCacheHub.mainSystem.getId());
				} catch (Exception e) {
					log.warn("\n<waring>!!!!\n<waring>!!!!\n 拉取[{}]子系统错误:{}",localCacheHub.mainSystem,ExceptionFormatUtils.buildExceptionMessages(e, 3));
					subSystems = new ArrayList<>(0);
				}
			}
			//
			BizSystem system;
			for (SubSystem sub : subSystems) {
				system = apiInstance.getSystemMetadata(sub.getCode());
				localCacheHub.systems.add(system);
				if(StringUtils.isBlank(sub.getProxyUrl())) {
					sub.setProxyUrl(system.getApiBaseUrl());
				}
				if(sub.isRouteMode()) { //路由注册模式
					buildMappingCacheData(localCacheHub,system, false);
				}else {
					log.info(" 开始注册子系统[{}]路由....",sub.getCode());
					BizSystemModule subSystemModule = new BizSystemModule();
					if(StringUtils.isBlank(sub.getProxyUrl())) {
						log.warn(" 子系统[{}]注册路由失败,原因：proxyUrl为空!!!!!!!",sub.getCode());
						continue;
					}
					subSystemModule.setProxyUri(WebUtils.getBaseUrl(sub.getProxyUrl()));
					//查询routes
					String url = sub.getProxyUrl() + "/app-management/module/compositeInfo";
					log.info(" 查询子系统路由请求地址：{}",url);
					CompositeModule compositeModule = null;
					try {
						compositeModule = HttpRequestEntity.get(url).internalCall().execute().toObject(CompositeModule.class);
					} catch (Exception e) {
						log.warn("\n<waring>!!!!\n<waring>!!!!\n 子系统[{}]注册路由失败,原因：{}",sub.getCode(),e.getMessage());
						continue;
					}
					subSystemModule.setSystemId(system.getId());
					subSystemModule.setServiceId(sub.getCode() + "-gateway");
					subSystemModule.setResolveRouteNames(compositeModule.getRoutes());
					subSystemModule.setSubGateway(true);
					subSystemModule.setStripPrefix(0);
					//移除忽略
					removeIgnoreRoute(localCacheHub, system, subSystemModule);
					if(subSystemModule.getFirstRouteName() != null) {
						buildRouteModuleMapping(localCacheHub,subSystemModule);
					}
					log.info(" 完成注册子系统[{}]路由！！！",sub.getCode());
				}
			}
		}
		log.info(" loadRemoteSystems END.");
	}
	
	private static void buildMappingCacheData(LocalCacheHub localCacheHub,BizSystem system,boolean isMainSystem) {
		BizSystemModule gatewayModule = null; //网关模块
		Iterator<BizSystemModule> iterator = system.getModules().iterator();
		while(iterator.hasNext()) {
			BizSystemModule module = iterator.next();
			if(!isMainSystem) {//移除忽略
				String routeName = module.getFirstRouteName();
				removeIgnoreRoute(localCacheHub, system, module);
				if(module.getFirstRouteName() == null) {
					log.info(">>>ignoreSubRoute:{} for module:{}",routeName,module.getServiceId());
					continue;
				}
			}
			if(!remoteRouteEnabled && !module.isLocal() && !module.isGlobal() && !module.isGateway()) {
				iterator.remove();
				log.info("> ignore remote module[{}-{}]!!!!!",module.getFirstRouteName(),module.getServiceId());
				continue;
			}
			if(StringUtils.isAnyBlank(module.getFirstRouteName(),module.getProxyUri())) {
				log.info("> ignore error module {}!!!!!",module);
				continue;
			}
			//
			if(isMainSystem && GlobalContext.APPID.equalsIgnoreCase(module.getServiceId())) {
				gatewayModule = module;
			}else {
				buildRouteModuleMapping(localCacheHub,module);
			}
		}
		//
		for (BizSystemPortal portal : system.getPortals()) {
			if(StringUtils.isNotBlank(portal.getDomain())) {
				localCacheHub.domainSystemMappings.put(portal.getDomain(), system);
				log.info("> add domainSystemMappings: {} = {}",portal.getDomain(), system.getId());
			}
		}
		//
		if(isMainSystem) {
			//网关本身
			if(gatewayModule == null) {
				gatewayModule = new BizSystemModule();
				gatewayModule.setServiceId(GlobalContext.APPID);
				system.getModules().add(gatewayModule);
			}
			gatewayModule.setRouteName(GlobalContext.APPID);
			gatewayModule.setStripPrefix(0);
			if(StringUtils.isBlank(gatewayModule.getAnonymousUris())) {
				gatewayModule.setAnonymousUris(ResourceUtils.getProperty(GatewayConfigs.GLOBAL_ANON_URI_CONFIG_KEY));
			}
			gatewayModule.setRouteName(GlobalContext.APPID);
			buildRouteModuleMapping(localCacheHub,gatewayModule);
		
		}
	}
	
	private static void buildRouteModuleMapping(LocalCacheHub localCacheHub,BizSystemModule module) {
		List<String> routeNames = module.getResolveRouteNames();
		for (String sub : routeNames) {
			if(localCacheHub.routeNames.contains(sub)) {
				log.warn("\n<waring>!!!!\n<waring>!!!!\n 路由[{}]已存在！！！模块[id:{},serviceId:{}]重复定义！！",sub,module.getId(),module.getServiceId());
			    return;
			}
			localCacheHub.routeModuleRelMappings.put(sub, module);
			localCacheHub.routeNames.add(sub);
		}
		localCacheHub.modules.add(module);
	}

	private static void loadLocalRouteModules(LocalCacheHub localCacheHub) {
		log.info(" loadLocalRouteModules BEGIN...");
		Properties properties = ResourceUtils.getAllProperties("spring.cloud.gateway.routes");
		Set<Entry<Object, Object>> entrySet = properties.entrySet();

		boolean withPathPrefix = StringUtils.isNotBlank(GatewayConfigs.PATH_PREFIX);
		BizSystemModule module;
		String prefix;
		outter:for (Entry<Object, Object> entry : entrySet) {
			try {
				if(entry.getKey().toString().endsWith(".id")) {
					prefix = entry.getKey().toString().replace(".id", "");
					module = new BizSystemModule();
					module.setId(properties.getProperty(prefix + ".moduleId"));
					module.setSystemId(properties.getProperty(prefix + ".systemId"));
					module.setServiceId(entry.getValue().toString());
					module.setProxyUri(properties.getProperty(prefix + ".uri"));
					PredicateDefinition pathPredicate = new PredicateDefinition(properties.getProperty(prefix + ".predicates[0]"));
					Collection<String> paths = pathPredicate.getArgs().values();
					List<String> routeList = new ArrayList<>(paths.size());
					for (String path : paths) {
						if(withPathPrefix && !path.startsWith(GatewayConfigs.PATH_PREFIX)) {
							log.warn(" route_format_error ->routeId:{},pathPredicateValue:{}\n -route path must startWith:{}",entry.getKey(),path,GatewayConfigs.PATH_PREFIX);
						    continue outter;
						}
						String routeName = path.substring(GatewayConfigs.PATH_PREFIX.length() + 1);
						routeName = routeName.substring(0,routeName.lastIndexOf("/"));
						routeList.add(routeName);
					}
					//
					if(!routeList.isEmpty()) {					
						module.setResolveRouteNames(routeList);
					}
					//
					FilterDefinition filterDefinition = new FilterDefinition(properties.getProperty(prefix + ".filters[0]"));
					String stripPrefix = filterDefinition.getArgs().get("_genkey_0");
					if(!StringUtils.isNumeric(stripPrefix)) {
						stripPrefix = "2";
					}
					module.setStripPrefix(Integer.parseInt(stripPrefix));
					module.setMetadata(parseRouteMetadata(prefix, properties));
					module.setAnonymousUris(properties.getProperty(prefix + ".anonymousUris"));
					module.setIgnoreApiPerm(Boolean.parseBoolean(properties.getProperty(prefix + ".ignoreApiPerm")));
					module.setSubGateway(module.getStripPrefix() == 0);
					module.setLocal(true);
					localCacheHub.mainSystem.getModules().add(module);
				}
			} catch (Exception e) {
				log.error("loadLocalRouteModule[{}] error:{} -> ",entry.getKey().toString(),ExceptionFormatUtils.buildExceptionMessages(e, 5));
			}
		}
		log.info(" loadLocalRouteModules END.");

	}
	
	private static Map<String, Object> parseRouteMetadata(String prefix,Properties properties){
		Map<String, Object> metadata = new HashMap<>();
		prefix = prefix + ".metadata.";
		Set<Entry<Object, Object>> entrySet = properties.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			String key  = Objects.toString(entry.getKey(), null);
			if(key == null || entry.getValue() == null || !entry.getKey().toString().startsWith(prefix)) {
				continue;
			}
			key = key.replace(prefix, "");
			metadata.put(key, entry.getValue());
		}
		return metadata;
	}
	
	private static void removeIgnoreRoute(LocalCacheHub localCacheHub,BizSystem system,BizSystemModule module) {
		if(!localCacheHub.ignoreSubRouteMappings.containsKey(system.getCode()))return;
		List<String> ignoreRoutes = localCacheHub.ignoreSubRouteMappings.get(system.getCode());
		if(module.getResolveRouteNames().size() == 1) {
			if(ignoreRoutes.contains(module.getFirstRouteName())) {
				module.setRouteName(null);
			}
		}else {
			List<String> routeNames = module.getResolveRouteNames();
			routeNames.removeAll(ignoreRoutes);
			module.setResolveRouteNames(routeNames);
		}
	}
	
	static class LocalCacheHub {
		
		BizSystem mainSystem; //主系统
		List<BizSystem> systems = new ArrayList<>();
		Map<String, BizSystem> domainSystemMappings = new HashMap<>();
		List<BizSystemModule> modules = new ArrayList<>();
		Map<String, BizSystemModule> routeModuleRelMappings = new HashMap<>();
		Map<String, KeyValuePair> openClientMappings = new HashMap<>();
		List<String>  routeNames = new ArrayList<>();
		Map<String, List<String>> ignoreSubRouteMappings = new HashMap<>(); //子系统忽略路由

		public void destory() {
			mainSystem = null;
			systems.clear();
			systems = null;
			domainSystemMappings.clear();
			domainSystemMappings = null;
			modules.clear();
			modules = null;
			routeModuleRelMappings.clear();
			routeModuleRelMappings = null;
			openClientMappings.clear();
			openClientMappings = null;
			routeNames.clear();
			ignoreSubRouteMappings.clear();
			ignoreSubRouteMappings = null;
		}
	}
}
