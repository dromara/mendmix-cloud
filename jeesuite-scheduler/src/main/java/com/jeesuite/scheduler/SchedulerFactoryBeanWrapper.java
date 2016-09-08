/**
 * 
 */
package com.jeesuite.scheduler;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年8月17日
 */
public class SchedulerFactoryBeanWrapper implements ApplicationContextAware,InitializingBean{
	
	protected static final Logger logger = LoggerFactory.getLogger(SchedulerFactoryBeanWrapper.class);

	private ApplicationContext context;
	
	private String groupName;

	List<AbstractJob> schedulers;
	
	public void setSingleMode(boolean singleMode){
		SchedulerContext.setSingleMode(singleMode);
	}
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public void setSchedulers(List<AbstractJob> schedulers) {
		this.schedulers = schedulers;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		
		Validate.notBlank(groupName);
		
		DefaultListableBeanFactory acf = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();
		
		List<Trigger> triggers = new ArrayList<>();
		for (AbstractJob sch : schedulers) {
			sch.setGroup(groupName);
			sch.init();
			triggers.add(registerSchedulerTriggerBean(acf,sch));
		}
		
		String beanName = "schedulerFactory";
		BeanDefinitionBuilder beanDefBuilder = BeanDefinitionBuilder.genericBeanDefinition(SchedulerFactoryBean.class);
		beanDefBuilder.addPropertyValue("triggers", triggers);
		acf.registerBeanDefinition(beanName, beanDefBuilder.getRawBeanDefinition());
		
		for (AbstractJob sch : schedulers) {
			if(sch.isExecuteOnStarted()){
				new Thread(new Runnable() {
					@Override
					public void run() {						
						logger.info("<<Job[{}] execute on startup....",sch.jobName);
						sch.execute();
						logger.info(">>Job[{}] execute on startup ok!",sch.jobName);
					}
				}).start();
			}
		}
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
		beanDefBuilder.addPropertyValue("concurrent", false);
		acf.registerBeanDefinition(jobDetailBeanName, beanDefBuilder.getRawBeanDefinition());
		
		//注册Trigger
		String triggerBeanName = sch.getJobName() + "Trigger";
		beanDefBuilder = BeanDefinitionBuilder.genericBeanDefinition(CronTriggerFactoryBean.class);
		beanDefBuilder.addPropertyReference("jobDetail", jobDetailBeanName);
		beanDefBuilder.addPropertyValue("cronExpression", sch.getCronExpr());
		beanDefBuilder.addPropertyValue("group", groupName);
		acf.registerBeanDefinition(triggerBeanName, beanDefBuilder.getRawBeanDefinition());
		
		logger.info("register scheduler task [{}] ok!!",sch.getJobName());
		return (Trigger) context.getBean(triggerBeanName);
		
	}

}
