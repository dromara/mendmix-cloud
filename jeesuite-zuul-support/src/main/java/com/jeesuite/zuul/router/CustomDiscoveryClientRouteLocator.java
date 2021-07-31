package com.jeesuite.zuul.router;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.zuul.CurrentSystemHolder;
import com.jeesuite.zuul.model.BizSystemModule;

/**
 * 
 * <br>
 * Class Name   : CustomDiscoveryClientRouteLocator
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年9月29日
 */
public class CustomDiscoveryClientRouteLocator extends DiscoveryClientRouteLocator {

	private final static Logger logger = LoggerFactory.getLogger(CustomDiscoveryClientRouteLocator.class);
	
	@Autowired(required = false)
	private DiscoveryClient discoveryClient;
	
	private Map<String, BizSystemModule> currentRouteModules = new HashMap<>();

	private volatile boolean refreshing = false;
	
	private long lastRefreshTime;
	
	public CustomDiscoveryClientRouteLocator(String servletPath, DiscoveryClient discovery, ZuulProperties properties,
			ServiceRouteMapper serviceRouteMapper, ServiceInstance localServiceInstance) {
		super(servletPath, discovery, properties, serviceRouteMapper, localServiceInstance);
	}


	@Override
	protected LinkedHashMap<String, ZuulRoute> locateRoutes() {
		LinkedHashMap<String, ZuulProperties.ZuulRoute> routesMap = new LinkedHashMap<>();
        if(refreshing)return routesMap;
		
		refreshing = true;
		try {
	        routesMap.putAll(super.locateRoutes());  
	        
	        if(currentRouteModules.isEmpty()){
	        	currentRouteModules = CurrentSystemHolder.getRouteModuleMappings();
	        }
	        if(currentRouteModules.isEmpty())return routesMap;

	        String path = null;
	        ZuulProperties.ZuulRoute zuulRoute = null;
	        for (BizSystemModule module : currentRouteModules.values()) {
	        	path = String.format("/%s/**", module.getRouteName());
	        	zuulRoute = new ZuulProperties.ZuulRoute();  
	        	zuulRoute.setPath(path);
	        	zuulRoute.setId(module.getRouteName());
	        	zuulRoute.setServiceId(module.getServiceId());
	        	
	        	List<ServiceInstance> instances = discoveryClient.getInstances(module.getServiceId());
	        	List<String> activeNodes = instances.stream().map(o -> o.getHost() + ":" + o.getPort()).collect(Collectors.toList());
	        	module.setActiveNodes(activeNodes);
	        	//
	        	routesMap.put(path, zuulRoute);
	      
	        	logger.info("add new Route:{} = {}",path,JsonUtils.toJson(zuulRoute));
			}
	        
	        return routesMap;
		} finally {
			refreshing = false;
		}
	}


	@Override
	public void refresh() {
		if(lastRefreshTime == 0 || System.currentTimeMillis() - lastRefreshTime > 30000){	
			Map<String, BizSystemModule> newestServiceModules = CurrentSystemHolder.getRouteModuleMappings();
			//
			if(newestServiceModules.size() != currentRouteModules.size() 
					|| !newestServiceModules.keySet().containsAll(currentRouteModules.keySet()) 
					|| !currentRouteModules.keySet().containsAll(newestServiceModules.keySet()) 
			){
				currentRouteModules = newestServiceModules;
				doRefresh();
			}
			lastRefreshTime = System.currentTimeMillis();
		}
	}
	

}
