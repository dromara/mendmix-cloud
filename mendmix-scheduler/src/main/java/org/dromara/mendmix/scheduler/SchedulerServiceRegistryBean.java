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
package org.dromara.mendmix.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.core.config.Order;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.common.util.SpringAopHelper;
import org.dromara.mendmix.scheduler.annotation.ScheduleConf;
import org.dromara.mendmix.spring.CommonApplicationEvent;
import org.dromara.mendmix.spring.InstanceFactory;
import org.dromara.mendmix.spring.SpringEventType;
import org.quartz.JobListener;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年8月17日
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class SchedulerServiceRegistryBean implements ApplicationContextAware,InitializingBean,DisposableBean,PriorityOrdered,ApplicationListener<CommonApplicationEvent>{
	
	private static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.scheduler");

	private ApplicationContext context;
	
	private String groupName;

	List<AbstractJob> schedulers = new ArrayList<>();
	
	private int threadPoolSize;
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
		JobContext.getContext().setGroupName(groupName);
	}

	public void setSchedulers(List<AbstractJob> schedulers) {
		this.schedulers = schedulers;
	}

	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	public void setPersistHandler(PersistHandler persistHandler) {
		JobContext.getContext().setPersistHandler(persistHandler);
	}
	
	public void setRegistry(JobRegistry registry) {
		JobContext.getContext().setRegistry(registry);
		logger.info("<startup-logging>  use jobRegistry:{}",registry.getClass().getName());
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
		InstanceFactory.setApplicationContext(applicationContext);;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if(!ResourceUtils.getBoolean("mendmix-cloud.task.enabled",true)) {
			return;
		}
		
		if(StringUtils.isBlank(groupName)) {
			setGroupName(GlobalContext.APPID);
		}
		
		Validate.notBlank(groupName);
		
		DefaultListableBeanFactory acf = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();
		
		Map<String, AbstractJob> jobBeans = InstanceFactory.getBeansOfType(AbstractJob.class);
		if(jobBeans == null || jobBeans.isEmpty()) {
			logger.warn("Scheduler init failed,Any job not found");
			return;
		}
		
		List<Trigger> triggers = new ArrayList<>();
		Map<String, String> cronExpressions = ResourceUtils.getMappingValues("mendmix-cloud.task.cronExpr");
		List<String> disableJobNames = ResourceUtils.getList("mendmix-cloud.task.disableJobNames");
		String jobName;
		for (AbstractJob job : jobBeans.values()) {
			
			AbstractJob unwrapJob = (AbstractJob) SpringAopHelper.getTarget(job);
            if(!unwrapJob.getClass().isAnnotationPresent(ScheduleConf.class))continue;
            ScheduleConf annotation = unwrapJob.getClass().getAnnotation(ScheduleConf.class);
        	jobName = annotation.jobName();
        	if(StringUtils.isBlank(jobName)) {
        		jobName = StringUtils.uncapitalize(unwrapJob.getClass().getSimpleName());
        	}
        	if(disableJobNames.contains(jobName)) {
        		logger.info("jobName[{}] is disabled!!!",jobName);
        		continue;
        	}
        	if(cronExpressions.containsKey(jobName)) {
        		job.setCronExpr(cronExpressions.get(jobName));
        	}else {
        		job.setCronExpr(annotation.cronExpr());
        	}
        	job.setExecuteOnStarted(annotation.executeOnStarted());
        	job.setGroup(groupName);
        	job.setJobName(jobName);
        	job.setJobAlias(StringUtils.defaultIfBlank(annotation.jobAlias(), jobName));
        	job.setRetries(annotation.retries());
        	if(!schedulers.contains(job)){  
        		job.setGroup(groupName);
        		job.init();
    			triggers.add(registerSchedulerTriggerBean(acf,job));
        		schedulers.add(job);
        		JobContext.getContext().addJob(unwrapJob);
        		logger.info("<startup-logging>  register new job:\n{}",JsonUtils.toPrettyJson(unwrapJob));
        	}else{
        		logger.info("<startup-logging>  Job[{}] is registered",job.getJobName());
        	}
            
		}
		
		SchedulerFactoryBean schFactory = null;
		try {schFactory = context.getBean(SchedulerFactoryBean.class);} catch (Exception e) {}
		if(schFactory == null){
			String beanName = "quartzScheduler";
			BeanDefinitionBuilder beanDefBuilder = BeanDefinitionBuilder.genericBeanDefinition(SchedulerFactoryBean.class);
			beanDefBuilder.addPropertyValue("triggers", triggers);
			JobListener[] jobListeners = {new JobStatusListener()};
			beanDefBuilder.addPropertyValue("globalJobListeners", jobListeners);
			//schFactory.setSchedulerListeners(schedulerListeners);
			//schFactory.setGlobalTriggerListeners(globalTriggerListeners);
			
			Properties quartzProperties = new Properties();
			threadPoolSize = threadPoolSize > 0 ? threadPoolSize : (schedulers.size() > 10 ? (schedulers.size()/2)  : schedulers.size());
			quartzProperties.setProperty(SchedulerFactoryBean.PROP_THREAD_COUNT, String.valueOf(threadPoolSize));
			beanDefBuilder.addPropertyValue("quartzProperties", quartzProperties);
			
			acf.registerBeanDefinition(beanName, beanDefBuilder.getRawBeanDefinition());
		}else{
			//schFactory.setTriggers(triggers.toArray(new Trigger[0]));
		}
		
		
		Collection<AbstractJob> jobs = JobContext.getAllJobs();
		for (AbstractJob job : jobs) {
			JobContext.getContext().submitSyncTask(new Runnable() {
				@Override
				public void run() {	
					InstanceFactory.waitUtilInitialized();
					job.afterInitialized();
					if(job.isExecuteOnStarted()){						
						logger.info("<startup-logging>  Job[{}] execute on startup....",job.jobName);
						job.execute();
						logger.info("<startup-logging>  Job[{}] execute on startup ok!",job.jobName);
					}
				}
			});
		}
		//
		if(JobContext.getContext().getPersistHandler() == null) {
			PersistHandler persistHandler = InstanceFactory.getInstance(PersistHandler.class);
			JobContext.getContext().setPersistHandler(persistHandler);
		}
		//
		JobContext.getContext().getRegistry().onRegistered();
	}

	/**
	 * 
	 * @param acf
	 * @param sch
	 * @return
	 */
	private Trigger registerSchedulerTriggerBean(DefaultListableBeanFactory acf,AbstractJob sch) {
		//注册JobDetail
		String jobDetailBeanName = sch.getJobName() + "JobDetail";
		if(context.containsBean(jobDetailBeanName)){
			throw new RuntimeException("duplicate jobName["+sch.getJobName()+"] defined!!");
		}
		BeanDefinitionBuilder beanDefBuilder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingJobDetailFactoryBean.class);
		beanDefBuilder.addPropertyValue("targetObject", sch);
		beanDefBuilder.addPropertyValue("targetMethod", "execute");
		beanDefBuilder.addPropertyValue("group", groupName);
		beanDefBuilder.addPropertyValue("concurrent", false);
		acf.registerBeanDefinition(jobDetailBeanName, beanDefBuilder.getRawBeanDefinition());
		
		//注册Trigger
		String triggerBeanName = sch.getJobName() + "Trigger";
		beanDefBuilder = BeanDefinitionBuilder.genericBeanDefinition(CronTriggerFactoryBean.class);
		beanDefBuilder.addPropertyReference("jobDetail", jobDetailBeanName);
		beanDefBuilder.addPropertyValue("cronExpression", sch.getCronExpr());
		beanDefBuilder.addPropertyValue("group", groupName);
		acf.registerBeanDefinition(triggerBeanName, beanDefBuilder.getRawBeanDefinition());
		
		return (Trigger) context.getBean(triggerBeanName);
	} 

	@Override
	public void destroy() throws Exception {
		JobContext.getContext().close();
	}
	
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public void onApplicationEvent(CommonApplicationEvent event) {
		if(event.getEventType() == SpringEventType.tenantDataSourceChanged) {
			AbstractJob.setTenantDataSourceKeys(event.getEventData());
		}
	}

}
