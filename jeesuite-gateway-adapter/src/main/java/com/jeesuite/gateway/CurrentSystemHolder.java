package com.jeesuite.gateway;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.gateway.api.SystemMgtApi;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.gateway.model.BizSystemPortal;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.springweb.client.GenericApiRequest;
import com.jeesuite.springweb.exporter.AppMetadataHolder;
import com.jeesuite.springweb.model.AppMetadata;

/**
 * <br>
 * Class Name : CurrentSystemHolder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年12月3日
 */
public class CurrentSystemHolder {
	
	private static Logger log = LoggerFactory.getLogger("com.jeesuite.gateway");


	private static AtomicReference<Map<String, BizSystemModule>> routeModuleMappings = new AtomicReference<>();

	private static List<BizSystemModule> localModules;

	private static List<String> routeNames;
	
	private static Map<String, Map<String, ApiInfo>> moduleApiInfos = new HashMap<>();
	

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
	
	public static BizSystemPortal getSystemPortal(String host) {
		return null;
	}

	public static synchronized void load() {
		if (localModules == null) {
			loadLocalRouteModules();
		}
		List<BizSystemModule> modules;
		try {
			modules = InstanceFactory.getInstance(SystemMgtApi.class).getSystemModules();
			if (!localModules.isEmpty()) {
				modules.addAll(localModules);
			}
		} catch (Exception e) {
			modules = localModules;
		}
		
		if(!modules.stream().anyMatch(o -> GlobalRuntimeContext.APPID.equalsIgnoreCase(o.getServiceId()))) {
			BizSystemModule module = new BizSystemModule();
			module.setServiceId(GlobalRuntimeContext.APPID);
			modules.add(module);
			localModules.add(module);
		}

		Map<String, BizSystemModule> _modules = new HashMap<>(modules.size());
		routeNames = new ArrayList<>(modules.size());
		for (BizSystemModule module : modules) {
			boolean isGateway = GlobalRuntimeContext.APPID.equalsIgnoreCase(module.getServiceId());
			if (!isGateway && StringUtils.isBlank(module.getRouteName())) {
				continue;
			}
			// 网关特殊处理
			if (isGateway) {
				module.setRouteName(GlobalRuntimeContext.APPID);
				if(module.getAnonymousUris() == null) {
					module.setAnonymousUris(ResourceUtils.getProperty("jeesuite.request.anonymous-uris"));
				}
			} else {
				routeNames.add(module.getRouteName());
			}
			
			module.buildAnonUriMatcher();
			//
			if(moduleApiInfos.containsKey(module.getServiceId())) {
				module.setApiInfos(moduleApiInfos.get(module.getServiceId()));
			}else {
				new Thread(() -> initModuleApiInfos(module)).start();
			}
			_modules.put(module.getRouteName(), module);
			
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
	
	private static void initModuleApiInfos(BizSystemModule module) {
		try {
			String url;
			if(module.getServiceId().startsWith("http")) {
				url = String.format("%s/metadata", module.getServiceId());
			}else {
				url = String.format("http://%s/metadata", module.getServiceId());
			}
			AppMetadata appMetadata;
			if(GlobalRuntimeContext.APPID.equals(module.getRouteName())) {
				appMetadata = AppMetadataHolder.getMetadata();
			}else {			
				appMetadata = new GenericApiRequest.Builder().requestUrl(url).responseClass(AppMetadata.class).backendInternalCall().build().waitResponse();
			}
			Map<String, ApiInfo> apiInfos = new HashMap<>(appMetadata.getApis().size());
			String uri;
			for (ApiInfo api : appMetadata.getApis()) {
				if(GlobalRuntimeContext.APPID.equals(module.getRouteName())) {
					uri = String.format("%s/%s", GlobalRuntimeContext.getContextPath(), api.getUrl());
				}else {
					uri = String.format("%s/%s/%s", GlobalRuntimeContext.getContextPath(), module.getRouteName(), api.getUrl());
				}
				uri = uri.replace("//", "/");
				apiInfos.put(uri, api);
			}
			module.setApiInfos(apiInfos);
			moduleApiInfos.put(module.getServiceId(), apiInfos);
		} catch (Exception e) {
			if(e instanceof JeesuiteBaseException && ((JeesuiteBaseException)e).getCode() == 404) {
				module.setApiInfos(new HashMap<>(0));
				moduleApiInfos.put(module.getServiceId(), module.getApiInfos());
			}else {
				log.warn(">> initModuleApiInfos error -> serviceId:{},error:{}",module.getServiceId(),e.getMessage());
			}
		}
	}
			
}
