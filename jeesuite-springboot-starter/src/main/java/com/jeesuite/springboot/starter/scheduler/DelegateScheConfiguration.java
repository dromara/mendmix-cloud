/**
 * 
 */
package com.jeesuite.springboot.starter.scheduler;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.jeesuite.scheduler.JobRegistry;
import com.jeesuite.scheduler.SchedulerFactoryBeanWrapper;
import com.jeesuite.scheduler.registry.NullJobRegistry;
import com.jeesuite.scheduler.registry.ZkJobRegistry;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年3月28日
 */
@Configuration
@EnableConfigurationProperties(SchedulerProperties.class)
@ConditionalOnClass(SchedulerFactoryBeanWrapper.class)
@AutoConfigureAfter(QuartzAutoConfiguration.class)
@ConditionalOnProperty(name="jeesuite.task.groupName")
public class DelegateScheConfiguration {

	@Autowired
	private SchedulerProperties cacheProperties;

	@Bean
	public JobRegistry jobRegistry(){
		if(StringUtils.isNotBlank(cacheProperties.getRegistryServers())){
			ZkJobRegistry registry = new ZkJobRegistry();
			registry.setZkServers(cacheProperties.getRegistryServers());
			return registry;
		}else{
			return new NullJobRegistry();
		}
	}
	
	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public SchedulerFactoryBeanWrapper schedulerFactoryBean(JobRegistry jobRegistry) {
		SchedulerFactoryBeanWrapper bean = new SchedulerFactoryBeanWrapper();
		bean.setGroupName(cacheProperties.getGroupName());
		bean.setThreadPoolSize(cacheProperties.getThreadPoolSize());
		bean.setRegistry(jobRegistry);
		bean.setScanPackages(cacheProperties.getScanPackages());
		return bean;
	}

}
