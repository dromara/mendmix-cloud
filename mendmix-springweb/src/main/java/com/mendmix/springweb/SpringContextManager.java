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

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.async.AsyncInitializer;
import com.mendmix.common.async.RetryAsyncTaskExecutor;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.logging.actionlog.ActionLogCollector;
import com.mendmix.logging.applog.LogProfileManager;
import com.mendmix.spring.ApplicationStartedListener;
import com.mendmix.spring.InstanceFactory;

/**
 * 
 * <br>
 * Class Name : SpringContextManager
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年9月13日
 */
public class SpringContextManager
		implements ApplicationContextAware, BeanDefinitionRegistryPostProcessor, DisposableBean, CommandLineRunner {

	@Override
	public void destroy() throws Exception {
		RetryAsyncTaskExecutor.close();
		ActionLogCollector.destroy();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		InstanceFactory.setApplicationContext(applicationContext);
		LogProfileManager.reload();
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
	}

	@Override
	public void run(String... args) throws Exception {
		InstanceFactory.loadFinished();
		// 启动完成
		GlobalRuntimeContext.startFinished();
		// 执行异步初始化
		Map<String, AsyncInitializer> asyncInitializers = InstanceFactory.getBeansOfType(AsyncInitializer.class);
		if (asyncInitializers != null) {
			for (AsyncInitializer initializer : asyncInitializers.values()) {
				initializer.process();
			}
		}
		Map<String, ApplicationStartedListener> interfaces = InstanceFactory
				.getBeansOfType(ApplicationStartedListener.class);
		if (interfaces != null) {
			for (ApplicationStartedListener listener : interfaces.values()) {
				listener.onApplicationStarted(InstanceFactory.getContext());
			}
		}
		//
		System.out.println("\n");
		if (System.getProperties().containsKey("_app_start_time_point")) {
			long startTime = Long.parseLong(System.getProperties().remove("_app_start_time_point").toString());
			long endTime = System.currentTimeMillis();
			long time = endTime - startTime;
			System.out.println("Start Using Time: " + time / 1000 + " s");
		}
		System.out.println("...............................................................");
		System.out.println("..................Service starts successfully (port:"
				+ ResourceUtils.getProperty("server.port") + ")..................");
		System.out.println("...............................................................");
	}

}
