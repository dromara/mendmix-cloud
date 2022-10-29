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
package com.mendmix.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.util.ClassUtils;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.util.ClassScanner;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.scheduler.annotation.ScheduleConf;
import com.mendmix.spring.InstanceFactory;
import com.mendmix.spring.helper.SpringAopHelper;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年8月17日
 */
public class SchedulerFactoryBeanWrapper implements ApplicationContextAware,InitializingBean,DisposableBean,PriorityOrdered{
	
	protected static final Logger logger = LoggerFactory.getLogger(SchedulerFactoryBeanWrapper.class);

	private ApplicationContext context;
	
	private String groupName;

	List<AbstractJob> schedulers = new ArrayList<>();
	
	private String scanPackages;
	
	private int threadPoolSize;
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
		JobContext.getContext().setGroupName(groupName);
	}

	public void setSchedulers(List<AbstractJob> schedulers) {
		this.schedulers = schedulers;
	}

	public void setScanPackages(String scanPackages) {
		this.scanPackages = scanPackages;
	}

	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	public void setPersistHandler(PersistHandler persistHandler) {
		JobContext.getContext().setPersistHandler(persistHandler);
	}
	
	public void setRegistry(JobRegistry registry) {
		JobContext.getContext().setRegistry(registry);
		logger.info("MENDMIX-TRACE-LOGGGING-->> JobRegistry:{}",JobRegistry.class.getSimpleName());
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
		InstanceFactory.setApplicationContext(context);
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if(ResourceUtils.getBoolean("mendmix.task.disabled", false)) {
			logger.info("MENDMIX-TRACE-LOGGGING-->> mendmix.task.disabled = {},Skip!!!",ResourceUtils.getBoolean("mendmix.task.disabled", false));
			return;
		}
		
		logger.info("MENDMIX-TRACE-LOGGGING-->> JobContextNodeId:{}",JobContext.getContext().getNodeId());
		if(groupName == null) {
			setGroupName(GlobalRuntimeContext.APPID);
		}
		Validate.notBlank(groupName);
		
		DefaultListableBeanFactory acf = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();
		
		if(StringUtils.isNotBlank(scanPackages)){
			String[] packages = org.springframework.util.StringUtils.tokenizeToStringArray(this.scanPackages, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
			scanAndRegisterAnnotationJobs(packages);
		}
		
		if(schedulers.isEmpty()){
			logger.warn("MENDMIX-TRACE-LOGGGING-->> Scheduler init failed,Any job not found");
			return;
		}
		
		List<Trigger> triggers = new ArrayList<>();
		for (AbstractJob sch : schedulers) {
			sch.setGroup(groupName);
			sch.init();
			triggers.add(registerSchedulerTriggerBean(acf,sch));
		}
		
		SchedulerFactoryBean schFactory = null;
		try {schFactory = context.getBean(SchedulerFactoryBean.class);} catch (Exception e) {}
		if(schFactory == null){
			String beanName = "quartzScheduler";
			BeanDefinitionBuilder beanDefBuilder = BeanDefinitionBuilder.genericBeanDefinition(SchedulerFactoryBean.class);
			beanDefBuilder.addPropertyValue("triggers", triggers);
			
			Properties quartzProperties = new Properties();
			threadPoolSize = threadPoolSize > 0 ? threadPoolSize : (schedulers.size() > 10 ? (schedulers.size()/2)  : schedulers.size());
			quartzProperties.setProperty(SchedulerFactoryBean.PROP_THREAD_COUNT, String.valueOf(threadPoolSize));
			beanDefBuilder.addPropertyValue("quartzProperties", quartzProperties);
			logger.info("MENDMIX-TRACE-LOGGGING-->> init Scheduler threadPoolSize:"+threadPoolSize);
			
			acf.registerBeanDefinition(beanName, beanDefBuilder.getRawBeanDefinition());
		}else{
			//schFactory.setTriggers(triggers.toArray(new Trigger[0]));
		}
		
		
		for ( AbstractJob sch : schedulers) {
			final AbstractJob job = (AbstractJob) SpringAopHelper.getTarget(sch);
			//
			JobContext.getContext().addJob(job);
			//
			JobContext.getContext().submitSyncTask(new Runnable() {
				@Override
				public void run() {	
					job.afterInitialized();
					if(job.isExecuteOnStarted()){						
						logger.info("MENDMIX-TRACE-LOGGGING-->> Job[{}] execute on startup....",job.jobName);
						job.execute();
						logger.info("MENDMIX-TRACE-LOGGGING-->> Job[{}] execute on startup ok!",job.jobName);
					}
				}
			});
			
			logger.info("MENDMIX-TRACE-LOGGGING-->> Job[{}][{}]-Class[{}]  initialized finish ",job.group,job.jobName,job.getClass().getName());
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
	
	private void scanAndRegisterAnnotationJobs(String[] scanBasePackages){
    	String RESOURCE_PATTERN = "/**/*.class";
    	Map<String, String> cronExpressions = ResourceUtils.getMappingValues("mendmix.task.cron");
    	ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    	for (String scanBasePackage : scanBasePackages) {
    		logger.info("MENDMIX-TRACE-LOGGGING-->> begin scan package [{}] with Annotation[ScheduleConf] jobs ",scanBasePackage);
    		try {
                String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ClassUtils.convertClassNameToResourcePath(scanBasePackage)
                        + RESOURCE_PATTERN;
                org.springframework.core.io.Resource[] resources = resourcePatternResolver.getResources(pattern);
                MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
                ClassScanner.whoUseMeReport();
                for (org.springframework.core.io.Resource resource : resources) {
                    if (resource.isReadable()) {
                        MetadataReader reader = readerFactory.getMetadataReader(resource);
                        String className = reader.getClassMetadata().getClassName();
                        Class<?> clazz = Class.forName(className);
                        
                        String jobName;
                        if(clazz.isAnnotationPresent(ScheduleConf.class)){
                        	ScheduleConf annotation = clazz.getAnnotation(ScheduleConf.class);
                        	jobName = StringUtils.uncapitalize(clazz.getSimpleName());
                        	AbstractJob job = null;
                        	try {
                        		job = (AbstractJob) context.getBean(clazz);
							} catch (Exception e) {
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
                        	if(!schedulers.contains(job)){                        		
                        		schedulers.add(job);
                        		logger.info("MENDMIX-TRACE-LOGGGING-->> register new job:{}",ToStringBuilder.reflectionToString(job, ToStringStyle.JSON_STYLE));
                        	}else{
                        		logger.info("MENDMIX-TRACE-LOGGGING-->> Job[{}] is registered",job.getJobName());
                        	}
                        }
                    }
                }
                logger.info("MENDMIX-TRACE-LOGGGING-->> scan package["+scanBasePackage+"] finished!");
            } catch (Exception e) {
            	if(e instanceof org.springframework.beans.factory.NoSuchBeanDefinitionException){
            		throw (org.springframework.beans.factory.NoSuchBeanDefinitionException)e;
            	}
            	logger.error("<<scan package["+scanBasePackage+"] error", e);
            }
		}
    	
	}  

	@Override
	public void destroy() throws Exception {
		JobContext.getContext().close();
	}
	
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
