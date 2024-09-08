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
package org.dromara.mendmix.scheduler.autoconfigure;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.scheduler.JobRegistry;
import org.dromara.mendmix.scheduler.SchedulerServiceRegistryBean;
import org.dromara.mendmix.scheduler.api.ScheduleApiServlet;
import org.dromara.mendmix.scheduler.registry.NullJobRegistry;
import org.dromara.mendmix.scheduler.registry.RedisJobRegistry;
import org.dromara.mendmix.scheduler.registry.ZkJobRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年3月28日
 */
@Configuration
@ConditionalOnClass(SchedulerServiceRegistryBean.class)
//@AutoConfigureAfter(QuartzAutoConfiguration.class)
public class SchedulerConfiguration {

	@Bean
	public JobRegistry jobRegistry(){
		String registryType = ResourceUtils.getProperty("mendmix-cloud.task.registryType");
		if("none".equals(registryType)) {
			return new NullJobRegistry();
		}
	
		boolean checkConfig = false;
		if(StringUtils.isBlank(registryType) || (checkConfig = "zookeeper".equals(registryType))) {
			String zkServers = ResourceUtils.getAnyProperty("mendmix-cloud.task.zkServers","mendmix-cloud.global.zkServers");
			if(StringUtils.isNotBlank(zkServers)){
				ZkJobRegistry registry = new ZkJobRegistry();
				registry.setZkServers(zkServers);
				return registry;
			}else if(checkConfig) {
				throw new RuntimeException("config[mendmix.task.zkServers] is required");
			}
		}
		
        if(StringUtils.isBlank(registryType) || (checkConfig = "redis".equals(registryType))) {
        	try {
				Class.forName("org.springframework.data.redis.core.StringRedisTemplate");
				if(ResourceUtils.containsAnyProperty("spring.redis.host","spring.redis.sentinel.nodes","spring.redis.cluster.nodes")) {
					return new RedisJobRegistry();
				}else if(checkConfig) {
					throw new RuntimeException("config[spring.redis.host] is required");
				}
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Bean[RedisTemplate] not found");
			}
		}
        
        return new NullJobRegistry();
	}
	
	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public SchedulerServiceRegistryBean schedulerServiceRegistryBean(JobRegistry jobRegistry) {
		SchedulerServiceRegistryBean bean = new SchedulerServiceRegistryBean();
		bean.setGroupName(GlobalContext.APPID);
		bean.setThreadPoolSize(ResourceUtils.getInt("mendmix-cloud.task.threadPoolSize", 2));
		bean.setRegistry(jobRegistry);
		return bean;
	}
	
	@Bean
	@ConditionalOnWebApplication(type = Type.SERVLET)
	public ServletRegistrationBean<ScheduleApiServlet> scheduleApiServlet() {
		ServletRegistrationBean<ScheduleApiServlet> servletRegistrationBean;
	    final ScheduleApiServlet servlet = new ScheduleApiServlet();
		servletRegistrationBean = new ServletRegistrationBean<>(servlet);
		servletRegistrationBean.addUrlMappings("/exporter/scheduler/*");
		Map<String, String> mappings = ResourceUtils.getMappingValues("mendmix-cloud.request.pathPrefix.mapping");
		String prefix;
		for (String packageName : mappings.keySet()) {
			prefix = mappings.get(packageName);
			if(!prefix.startsWith("/")) {
				prefix = "/" + prefix;
			}
			String uriPattern = prefix + "/exporter/scheduler/*";
			servletRegistrationBean.addUrlMappings(uriPattern);
		}
	    return servletRegistrationBean;
	}

}
