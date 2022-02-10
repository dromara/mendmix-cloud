package com.jeesuite.springweb;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.jeesuite.common.async.RetryAsyncTaskExecutor;
import com.jeesuite.logging.integrate.ActionLogCollector;
import com.jeesuite.spring.InstanceFactory;

/**
 * 
 * <br>
 * Class Name   : SpringContextManager
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年9月13日
 */
public class SpringContextManager implements ApplicationContextAware,BeanDefinitionRegistryPostProcessor,DisposableBean{

	@Override
	public void destroy() throws Exception {
		RetryAsyncTaskExecutor.close();
		ActionLogCollector.destroy();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		InstanceFactory.setApplicationContext(applicationContext);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {}

}
