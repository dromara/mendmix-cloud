/*
 * Copyright 2016-2022 www.jeesuite.com.
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
package com.jeesuite.gateway.router;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.event.EnableBodyCachingEvent;
import org.springframework.cloud.gateway.filter.AdaptCachedBodyGlobalFilter;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.support.NotFoundException;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.util.JsonUtils;
import com.jeesuite.gateway.CurrentSystemHolder;
import com.jeesuite.gateway.GatewayConstants;
import com.jeesuite.gateway.model.BizSystemModule;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 * <br>
 * Class Name   : CustomRouteDefinitionRepository
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
	@Autowired
	private GatewayProperties gatewayProperties;

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		if (routeHub.get() == null) {
			routeHub.set(new HashMap<>());
		}
		
		if(routeHub.get().isEmpty()) {
			// 本地路由
			List<RouteDefinition> routes = gatewayProperties.getRoutes();
			for (RouteDefinition routeDef : routes) {
				routeHub.get().put(routeDef.getId().toLowerCase(), routeDef);
			}
			//
			Map<String, BizSystemModule> modules = CurrentSystemHolder.getRouteModuleMappings();
			for (BizSystemModule module : modules.values()) {
				// 网关本身
				if (GlobalRuntimeContext.APPID.equals(module.getServiceId())) {
					continue;
				}
				// 本地已加载
				if (routeHub.get().containsKey(module.getServiceId())) {
					continue;
				}
				//
				loadDynamicRouteDefinition(module);
			}
			
			for (RouteDefinition route : routeHub.get().values()) {
				EnableBodyCachingEvent enableBodyCachingEvent = new EnableBodyCachingEvent(new Object(), route.getId());
				adaptCachedBodyGlobalFilter.onApplicationEvent(enableBodyCachingEvent);
			}
			logger.info("\n=============load routes==============\n{}", JsonUtils.toPrettyJson(routeHub.get().values()));
		}

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

	public void loadDynamicRouteDefinition(BizSystemModule module) {
		String proxyUri = module.getProxyUri();
		int stripPrefix = StringUtils.countMatches(module.getRouteName(), "/") + 2;
		if(proxyUri.endsWith("/" + module.getRouteName())) {
			proxyUri = proxyUri.substring(0,proxyUri.lastIndexOf(module.getRouteName()) - 1);
			stripPrefix = 1;
		}
		RouteDefinition routeDef = new RouteDefinition();
		routeDef.setId(module.getServiceId());
		routeDef.setUri(URI.create(proxyUri));
		routeDef.setPredicates(new ArrayList<>(1));
		String pathExpr = String.format("Path=%s/%s/**", GatewayConstants.PATH_PREFIX, module.getRouteName());
		routeDef.getPredicates().add(new PredicateDefinition(pathExpr));
		routeDef.setFilters(new ArrayList<>(1));
		routeDef.getFilters().add(new FilterDefinition("StripPrefix=" + stripPrefix));
		routeHub.get().put(routeDef.getId(), routeDef);
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


}
