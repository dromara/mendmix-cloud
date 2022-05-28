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
package com.mendmix.springweb;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.mendmix.common.async.RetryAsyncTaskExecutor;
import com.mendmix.logging.integrate.ActionLogCollector;
import com.mendmix.spring.InstanceFactory;

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
