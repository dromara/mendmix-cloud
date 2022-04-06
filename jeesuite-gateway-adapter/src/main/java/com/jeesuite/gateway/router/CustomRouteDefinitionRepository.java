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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.EnableBodyCachingEvent;
import org.springframework.cloud.gateway.filter.AdaptCachedBodyGlobalFilter;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.support.NotFoundException;

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
public class CustomRouteDefinitionRepository implements RouteDefinitionRepository,InitializingBean {

	private static AtomicReference<Map<String, RouteDefinition>> routeHub = new AtomicReference<>();

	@Autowired
    private AdaptCachedBodyGlobalFilter adaptCachedBodyGlobalFilter;
	
	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
        //TODO 
        return Flux.fromIterable(routeHub.get().values());
	}

	@Override
	public Mono<Void> save(Mono<RouteDefinition> routes) {

		Map<String, RouteDefinition> map = new HashMap<>();
		routes.flatMap(r -> {
			map.put(r.getId(), r);
			return Mono.empty();
		});	
		routeHub.set(map);
	
		return Mono.empty();
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


	@Override
	public void afterPropertiesSet() throws Exception {
		Collection<RouteDefinition> routes = routeHub.get().values();
		for (RouteDefinition route : routes) {
			EnableBodyCachingEvent enableBodyCachingEvent = new EnableBodyCachingEvent(new Object(), route.getId());
            adaptCachedBodyGlobalFilter.onApplicationEvent(enableBodyCachingEvent);
		}
	}

}
