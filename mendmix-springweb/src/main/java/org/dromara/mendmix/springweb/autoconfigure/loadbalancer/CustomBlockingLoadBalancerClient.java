/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.springweb.autoconfigure.loadbalancer;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.dromara.mendmix.common.http.HostMappingHolder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer.Factory;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date May 25, 2022
 */
public class CustomBlockingLoadBalancerClient  extends BlockingLoadBalancerClient {

	private Map<String, ServiceInstance> fixedServiceInstances = new ConcurrentHashMap<>();
	
	public CustomBlockingLoadBalancerClient(Factory<ServiceInstance> loadBalancerClientFactory) {
		super(loadBalancerClientFactory);
	}

	@Override
	public <T> ServiceInstance choose(String serviceId, Request<T> request) {
        
		ServiceInstance instance = super.choose(serviceId, request);
		if(instance == null && HostMappingHolder.containsProxyUrlMapping(serviceId)) {
			instance = fixedServiceInstances.get(serviceId);
			if(instance == null) {
				synchronized (fixedServiceInstances) {
					URI uri = URI.create(HostMappingHolder.getProxyUrlMapping(serviceId));
					instance = new FixedServiceInstance(serviceId, uri);
					fixedServiceInstances.put(serviceId, instance);
				}
			}
		}
		return instance;
	}

	@Override
	public URI reconstructURI(ServiceInstance serviceInstance, URI original) {
		if(serviceInstance instanceof FixedServiceInstance) {
			return URI.create(HostMappingHolder.resolveUrl(original.toString()));
		}
		return super.reconstructURI(serviceInstance, original);
	}
	
	

}
