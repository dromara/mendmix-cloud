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
package org.dromara.mendmix.gateway.router;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.model.NameValuePair;
import org.dromara.mendmix.gateway.CurrentSystemHolder;
import org.dromara.mendmix.gateway.GatewayConfigs;
import org.dromara.mendmix.gateway.model.BizSystemModule;
import org.dromara.mendmix.spring.CommonApplicationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationListener;

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
public class CustomRouteDefinitionRepository implements RouteDefinitionRepository, ApplicationListener<CommonApplicationEvent> {

	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.adapter.gateway");

	private static AtomicReference<Map<String, RouteDefinition>> routeHub = new AtomicReference<>();

	private volatile boolean refreshable = true;

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		if(!refreshable) {
			return Flux.fromIterable(routeHub.get().values());
		}
		
		List<BizSystemModule> modules = CurrentSystemHolder.getAndLoadModules();
		Map<String, RouteDefinition> routeDefs = new HashMap<>(modules.size());
		List<String> existsPathDefs = new ArrayList<>();
		for (BizSystemModule module : modules) {
			// 网关本身
			if (GlobalContext.APPID.equals(module.getServiceId())) {
				continue;
			}

			if (routeDefs.containsKey(module.getServiceId())) {
				continue;
			}
			loadDynamicRouteDefinition(module,routeDefs,existsPathDefs);
		}
		
		if(!routeDefs.isEmpty()) {
			routeHub.set(routeDefs);
			StringBuilder message = new StringBuilder("\n================final RouteMapping begin===============\n");
			for (RouteDefinition route : routeHub.get().values()) {
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

	public void loadDynamicRouteDefinition(BizSystemModule module, Map<String, RouteDefinition> routeDefs,
			List<String> existsPathDefs) {
		String proxyUri = module.getProxyUri();
		int stripPrefix = module.getStripPrefix();
		RouteDefinition routeDef = new RouteDefinition();
		if (module.isSubGateway()) {
			routeDef.setId(module.getServiceId());
		} else {
			String id = String.format("%s_%s_%s", module.getServiceId(), module.getFirstRouteName(),
					StringUtils.trimToEmpty(module.getId()));
			routeDef.setId(id);
		}
		routeDef.setUri(URI.create(proxyUri));
		routeDef.setPredicates(new ArrayList<>(1));
		List<String> subRoutes = module.getResolveRouteNames();
		StringBuilder pathBuilder = new StringBuilder("Path=");
		String pathDefinition;
		for (String sub : subRoutes) {
			if (stripPrefix == 0 && ("/" + sub).startsWith(GatewayConfigs.PATH_PREFIX + "/")) {
				pathDefinition = String.format("/%s/**", sub);
			} else {
				pathDefinition = String.format("%s/%s/**", GatewayConfigs.PATH_PREFIX, sub);
			}
			if (existsPathDefs.contains(pathDefinition)) {
				logger.warn("<startup-logging>  忽略重复转发路径:{} for module:{}", pathDefinition,
						module.getServiceId());
				continue;
			}
			pathBuilder.append(pathDefinition);
			pathBuilder.append(",");
			//
			existsPathDefs.add(pathDefinition);
		}
		// 无任何path
		if (pathBuilder.length() == 5)
			return;

		pathBuilder.deleteCharAt(pathBuilder.length() - 1);
		routeDef.getPredicates().add(new PredicateDefinition(pathBuilder.toString()));
		routeDef.setFilters(new ArrayList<>(1));
		routeDef.getFilters().add(new FilterDefinition("StripPrefix=" + stripPrefix));
		if (!module.getMetadata().isEmpty()) {
			routeDef.getMetadata().putAll(module.getMetadata());
		}
		module.setRouteDefinition(routeDef);
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
			routeName = argValue.substring(GatewayConfigs.PATH_PREFIX.length() + 1);
			routeName = routeName.substring(0,routeName.lastIndexOf("/"));
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
	
	public void reload() {
		refreshable = true;
		CurrentSystemHolder.reload();
		getRouteDefinitions();
	}
	
	@Override
	public void onApplicationEvent(CommonApplicationEvent event) {
		if(routeHub.get() == null) {
			return;
		}
		List<NameValuePair> configs = event.getEventData();
		if (configs.stream().anyMatch(o -> o.getName().startsWith("spring.cloud.gateway.routes"))) {
			reload();
			logger.info(">> 收到ConfigChangeEvent,route refresh ok");
		}
	}

}
