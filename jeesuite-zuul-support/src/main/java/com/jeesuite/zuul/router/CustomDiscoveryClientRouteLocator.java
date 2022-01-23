package com.jeesuite.zuul.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;
import com.jeesuite.zuul.CurrentSystemHolder;
import com.jeesuite.zuul.model.BizSystemModule;

/**
 * 
 * <br>
 * Class Name : CustomDiscoveryClientRouteLocator
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年9月29日
 */
public class CustomDiscoveryClientRouteLocator extends DiscoveryClientRouteLocator  {

	private final static Logger logger = LoggerFactory.getLogger(CustomDiscoveryClientRouteLocator.class);

	private ZuulProperties properties;

	private Map<String, BizSystemModule> currentRouteModules = new HashMap<>();
	private Map<String, ZuulProperties.ZuulRoute> localRoutes;

	public CustomDiscoveryClientRouteLocator(String servletPath, DiscoveryClient discovery, ZuulProperties properties,
			ServiceRouteMapper serviceRouteMapper, ServiceInstance localServiceInstance) {
		super(servletPath, discovery, properties, serviceRouteMapper, localServiceInstance);
		this.properties = properties;
	}


	@Override
	protected LinkedHashMap<String, ZuulRoute> locateRoutes() {
		LinkedHashMap<String, ZuulProperties.ZuulRoute> routesMap = new LinkedHashMap<>();
		// 从远程加载路由信息
		Map<String, ZuulRoute> remoteRoutes = buildRemoteRoutes();

		// 从application.properties中加载路由信息
		if (localRoutes == null) {
			List<String> remoteRoutePaths = new ArrayList<>(remoteRoutes.keySet());
			localRoutes = new HashMap<>();
			for (ZuulRoute route : this.properties.getRoutes().values()) {
				if (!remoteRoutePaths.contains(route.getPath())) {
					localRoutes.put(route.getPath(), route);
				}
			}
		}

		if (!localRoutes.isEmpty()) {
			routesMap.putAll(localRoutes);
		}
		routesMap.putAll(remoteRoutes);

		logger.info(">>load locateRoutes:{}", routesMap);
		return routesMap;
	}

	@Override
	public void refresh() {
		Map<String, BizSystemModule> newestServiceModules = CurrentSystemHolder.getRouteModuleMappings();
		//
		if (newestServiceModules.size() != currentRouteModules.size()
				|| !newestServiceModules.keySet().containsAll(currentRouteModules.keySet())
				|| !currentRouteModules.keySet().containsAll(newestServiceModules.keySet())) {
			currentRouteModules = newestServiceModules;
			doRefresh();
		}
	}

	private Map<String, ZuulProperties.ZuulRoute> buildRemoteRoutes() {

		if (currentRouteModules == null) {
			currentRouteModules = CurrentSystemHolder.getRouteModuleMappings();
		}

		Collection<BizSystemModule> modules = currentRouteModules.values();
		Map<String, ZuulProperties.ZuulRoute> routes = new HashMap<>();
		String path = null;
		ZuulProperties.ZuulRoute zuulRoute = null;
		for (BizSystemModule module : modules) {
			if(GlobalRuntimeContext.APPID.equalsIgnoreCase(module.getServiceId()))continue;
			if (StringUtils.isBlank(module.getServiceId()) && StringUtils.isBlank(module.getProxyUrl())) {
				continue;
			}
			path = String.format("/%s/**", module.getRouteName());
			zuulRoute = new ZuulProperties.ZuulRoute();
			zuulRoute.setPath(path);
			zuulRoute.setId(module.getRouteName());
			if (StringUtils.isNotBlank(module.getProxyUrl())) {
				zuulRoute.setUrl(module.getProxyUrl());
			} else {
				zuulRoute.setServiceId(module.getServiceId());
			}
			//
			routes.put(path, zuulRoute);
		}

		return routes;
	}

}
