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
package com.mendmix.gateway.router;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.EnableBodyCachingEvent;
import org.springframework.cloud.gateway.filter.AdaptCachedBodyGlobalFilter;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.support.NotFoundException;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.gateway.CurrentSystemHolder;
import com.mendmix.gateway.GatewayConfigs;
import com.mendmix.gateway.model.BizSystemModule;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 * <br>
 * Class Name : CustomRouteDefinitionRepository
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Apr 5, 2022
 */
public class CustomRouteDefinitionRepository implements RouteDefinitionRepository {

	private final static Logger logger = LoggerFactory.getLogger("com.zvosframework.adapter.gateway");

	private static AtomicReference<Map<String, RouteDefinition>> routeHub = new AtomicReference<>();

	@Autowired
	private AdaptCachedBodyGlobalFilter adaptCachedBodyGlobalFilter;
	// @Autowired
	// private GatewayProperties gatewayProperties;
	
	private volatile boolean refreshable = true;

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		if(!refreshable) {
			return Flux.fromIterable(routeHub.get().values());
		}
		
		Map<String, BizSystemModule> modules = CurrentSystemHolder.getRouteModuleMappings();
		Map<String, RouteDefinition> routeDefs = new HashMap<>(modules.size());
		for (BizSystemModule module : modules.values()) {
			// 网关本身
			if (GlobalRuntimeContext.APPID.equals(module.getServiceId())) {
				continue;
			}

			if (routeDefs.containsKey(module.getServiceId())) {
				continue;
			}
			loadDynamicRouteDefinition(routeDefs,module);
		}
		
		if(!routeDefs.isEmpty()) {
			routeHub.set(routeDefs);
			StringBuilder message = new StringBuilder("\n================final RouteMapping begin===============\n");
			for (RouteDefinition route : routeHub.get().values()) {
				EnableBodyCachingEvent enableBodyCachingEvent = new EnableBodyCachingEvent(new Object(), route.getId());
				adaptCachedBodyGlobalFilter.onApplicationEvent(enableBodyCachingEvent);
				buildRouteLogMessage(message, route);
			}
			message.append("================final RouteMapping end===============\n");
			logger.info(message.toString());
		}else {
			routeHub.set(new HashMap<>());
		}
		
		refreshable = false;

		return Flux.fromIterable(routeHub.get().values());
	}

	@Override
	public Mono<Void> save(Mono<RouteDefinition> routes) {

		if (routeHub.get() == null) {
			routeHub.set(new HashMap<>());
		}
		routes.flatMap(routeDef -> {
			routeHub.get().put(routeDef.getId(), routeDef);
			return Mono.empty();
		});

		return Mono.empty();
	}

	public void loadDynamicRouteDefinition(Map<String, RouteDefinition> routeDefs,BizSystemModule module) {
		String proxyUri = module.getProxyUri();
		int stripPrefix = module.getStripPrefix();
		RouteDefinition routeDef = new RouteDefinition();
		routeDef.setId(module.getRouteName());
		routeDef.setUri(URI.create(proxyUri));
		routeDef.setPredicates(new ArrayList<>(1));
		String pathExpr = String.format("Path=%s/%s/**", GatewayConfigs.PATH_PREFIX, module.getRouteName());
		routeDef.getPredicates().add(new PredicateDefinition(pathExpr));
		routeDef.setFilters(new ArrayList<>(1));
		routeDef.getFilters().add(new FilterDefinition("StripPrefix=" + stripPrefix));
		routeDefs.put(routeDef.getId(), routeDef);
	}

	@Override
	public Mono<Void> delete(Mono<String> routeId) {
		return routeId.flatMap(id -> {
			if (routeHub.get() != null && routeHub.get().containsKey(id)) {
				routeHub.get().remove(id);
				return Mono.empty();
			}
			return Mono.defer(() -> Mono.error(new NotFoundException("RouteDefinition not found: " + routeId)));
		});
	}

	private void buildRouteLogMessage(StringBuilder message, RouteDefinition route) {
		message.append(route.getId()).append(":[");
		message.append("url:").append(route.getUri()).append(",");
		PredicateDefinition pathDef = route.getPredicates().stream().filter(p -> "Path".equals(p.getName())).findFirst()
				.orElse(null);
		String routeName = "";
		if (pathDef != null) {
			String argValue = pathDef.getArgs().get("_genkey_0");
			String[] parts = StringUtils.split(argValue, "/");
			for (int i = 1; i < parts.length - 1; i++) {
				routeName = routeName + parts[i] + "/";
			}
			routeName = routeName.substring(0, routeName.length() - 1);
			message.append("routeName:").append(routeName);
			message.append(",path:").append(argValue);
		}
		FilterDefinition stripPrefixDef = route.getFilters().stream().filter(p -> "StripPrefix".equals(p.getName()))
				.findFirst().orElse(null);
		if (stripPrefixDef != null) {
			message.append(",stripPrefix:").append(stripPrefixDef.getArgs().get("_genkey_0"));
		}

		message.append("]\n");
	}

}
