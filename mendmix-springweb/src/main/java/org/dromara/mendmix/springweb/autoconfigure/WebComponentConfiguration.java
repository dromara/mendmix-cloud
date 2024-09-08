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
package org.dromara.mendmix.springweb.autoconfigure;

import org.apache.commons.lang3.RandomUtils;
import org.dromara.mendmix.common.WorkerIdGenerator;
import org.dromara.mendmix.common.sequence.SequenceGenerateService;
import org.dromara.mendmix.common.task.GlobalInternalScheduleService;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.springweb.autoconfigure.loadbalancer.CustomBlockingLoadBalancerClient;
import org.dromara.mendmix.springweb.client.LoadBalancerWrapper;
import org.dromara.mendmix.springweb.client.SimpleRestTemplateBuilder;
import org.dromara.mendmix.springweb.component.workerid.LocalWorkerIdGenerator;
import org.dromara.mendmix.springweb.component.workerid.RedisWorkerIdGenerator;
import org.dromara.mendmix.springweb.component.workerid.ZkWorkerIdGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebComponentConfiguration {

	@Bean("restTemplate")
	@LoadBalanced
	RestTemplate restTemplate() {
		int readTimeout = ResourceUtils.getInt("mendmix-cloud.httpclient.readTimeout.ms", 30000);
		return SimpleRestTemplateBuilder.build(readTimeout,true);
	}
	
	@Bean
	public LoadBalancerClient blockingLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory) {
		return new CustomBlockingLoadBalancerClient(loadBalancerClientFactory);
	}
	
	
	@Bean
	public LoadBalancerWrapper loadBalancerWrapper(DiscoveryClient discoveryClient) {
		return new LoadBalancerWrapper(discoveryClient);
	}
	
	@Bean
    public WorkerIdGenerator workIdGenerator() {
    	if(ResourceUtils.containsProperty("mendmix-cloud.zookeeper.servers")) {
    		try {
				Class.forName("org.apache.zookeeper.ZooKeeper");
				return new ZkWorkerIdGenerator();
			} catch (ClassNotFoundException e) {}
    	}
    	if(ResourceUtils.containsAnyProperty("spring.redis.host","spring.redis.sentinel.nodes","spring.redis.cluster.nodes")){
    		try {
				Class.forName("org.springframework.data.redis.core.StringRedisTemplate");
				return new RedisWorkerIdGenerator();
			} catch (ClassNotFoundException e) {}
    	}
    	try {
			Class.forName("org.apache.commons.io.FileUtils");
			return new LocalWorkerIdGenerator();
		} catch (ClassNotFoundException e) {
			return new WorkerIdGenerator() {
				@Override
				public int generate(String nodeId) {
					return RandomUtils.nextInt(10, 99);
				}
			};
		}
    }
    
    @Bean
	public GlobalInternalScheduleService globalInternalScheduleService() {
		return new GlobalInternalScheduleService();
	}
    
    @Bean
    @ConditionalOnProperty(name = "mendmix-cloud.common.sequence.enaled",havingValue = "true")
	public SequenceGenerateService sequenceGenerateService() {
    	return new SequenceGenerateService();
    }

}
