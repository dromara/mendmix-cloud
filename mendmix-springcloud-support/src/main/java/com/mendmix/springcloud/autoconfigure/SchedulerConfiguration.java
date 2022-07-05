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
package com.mendmix.springcloud.autoconfigure;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.scheduler.JobRegistry;
import com.mendmix.scheduler.SchedulerFactoryBeanWrapper;
import com.mendmix.scheduler.registry.NullJobRegistry;
import com.mendmix.scheduler.registry.RedisJobRegistry;
import com.mendmix.scheduler.registry.ZkJobRegistry;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年3月28日
 */
@Configuration
@ConditionalOnClass(SchedulerFactoryBeanWrapper.class)
//@AutoConfigureAfter(QuartzAutoConfiguration.class)
@ConditionalOnProperty(name="mendmix.task.scanPackages")
public class SchedulerConfiguration {

	@Bean
	public JobRegistry jobRegistry(){
		String zkServers = ResourceUtils.getAnyProperty("mendmix.task.zkServers","mendmix.global.zkServers");
		if(StringUtils.isNotBlank(zkServers)){
			ZkJobRegistry registry = new ZkJobRegistry();
			registry.setZkServers(zkServers);
			return registry;
		}else{
			try {
				Class.forName("org.springframework.data.redis.core.StringRedisTemplate");
				if(ResourceUtils.containsAnyProperty("spring.redis.host","spring.redis.sentinel.nodes","spring.redis.cluster.nodes")) {
					return new RedisJobRegistry();
				}
			} catch (ClassNotFoundException e) {
			}
			return new NullJobRegistry();
		}
	}
	
	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public SchedulerFactoryBeanWrapper schedulerFactoryBean(JobRegistry jobRegistry) {
		SchedulerFactoryBeanWrapper bean = new SchedulerFactoryBeanWrapper();
		bean.setGroupName(GlobalRuntimeContext.APPID);
		bean.setThreadPoolSize(ResourceUtils.getInt("mendmix.task.threadPoolSize", 2));
		bean.setRegistry(jobRegistry);
		bean.setScanPackages(ResourceUtils.getProperty("mendmix.task.scanPackages"));
		return bean;
	}

}
