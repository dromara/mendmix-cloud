/**
 * 
 */
package com.jeesuite.springboot.autoconfigure;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.util.ResourceUtils;
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
@ConditionalOnClass(SchedulerFactoryBeanWrapper.class)
//@AutoConfigureAfter(QuartzAutoConfiguration.class)
@ConditionalOnProperty(name="jeesuite.task.scanPackages")
public class SchedulerConfiguration {

	@Bean
	public JobRegistry jobRegistry(){
		String zkServers = ResourceUtils.getAnyProperty("jeesuite.task.zkServers","jeesuite.global.zkServers");
		if(StringUtils.isNotBlank(zkServers)){
			ZkJobRegistry registry = new ZkJobRegistry();
			registry.setZkServers(zkServers);
			return registry;
		}else{
			return new NullJobRegistry();
		}
	}
	
	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public SchedulerFactoryBeanWrapper schedulerFactoryBean(JobRegistry jobRegistry) {
		String groupName = ResourceUtils.getProperty("jeesuite.task.groupName",GlobalRuntimeContext.APPID);
		SchedulerFactoryBeanWrapper bean = new SchedulerFactoryBeanWrapper();
		bean.setGroupName(groupName);
		bean.setThreadPoolSize(ResourceUtils.getInt("jeesuite.task.threadPoolSize", 2));
		bean.setRegistry(jobRegistry);
		bean.setScanPackages(ResourceUtils.getProperty("jeesuite.task.scanPackages"));
		return bean;
	}

}
