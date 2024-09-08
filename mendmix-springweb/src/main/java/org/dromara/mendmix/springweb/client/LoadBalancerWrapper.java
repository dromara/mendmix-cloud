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
package org.dromara.mendmix.springweb.client;

import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.dromara.mendmix.common.http.HostMappingHolder;
import org.dromara.mendmix.common.http.ProxyResolver;
import org.dromara.mendmix.spring.InstanceFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

/**
 * 
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2021年10月23日
 */
public class LoadBalancerWrapper implements ProxyResolver,CommandLineRunner {

	private boolean customLoadBalance;
	private static LoadBalancerWrapper me;
	private LoadBalancerClient loadBalancer;
	private DiscoveryClient discoveryClient;

	public LoadBalancerWrapper(DiscoveryClient discoveryClient) {
		try {
			Class.forName("org.springframework.web.reactive.DispatcherHandler");
			Class.forName("reactor.core.publisher.Mono");
			customLoadBalance = true;
		} catch (ClassNotFoundException e) {
			customLoadBalance = false;
		}
		this.discoveryClient = discoveryClient;
		LoadBalancerWrapper.me = this;
		HostMappingHolder.setProxyResolver(this);
	}

    //LoadBalancerClient,在未启动完成执行回出现循环依赖异常
	public static String choose(String serviceId) {
		ServiceInstance selected = null;
		if(me.loadBalancer != null) {
			selected = me.loadBalancer.choose(serviceId);
		}else if(me.discoveryClient != null){
			List<ServiceInstance> instances = me.discoveryClient.getInstances(serviceId);
			if(instances != null && !instances.isEmpty()) {
				if(instances.size() == 1) {
			      selected = instances.get(0);
			    }else {
			      selected = instances.get(RandomUtils.nextInt(0, instances.size()));
			   }
			}
		}
		
	   if(selected == null)return null;

   	   return selected.getUri().toString();
	}

	@Override
	public String resolve(String origin) {
		return choose(origin);
	}

	@Override
	public void run(String... args) throws Exception {
		if(!customLoadBalance) {			
			loadBalancer = InstanceFactory.getInstance(LoadBalancerClient.class);
		}
	}

	
}
