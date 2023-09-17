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
package com.mendmix.spring;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

import com.mendmix.spring.helper.EnvironmentHelper;

public class InstanceFactory {

	private static ApplicationContext applicationContext;
	private static Long timeStarting = System.currentTimeMillis();
	private static AtomicBoolean initialized = new AtomicBoolean(false);
	private static AtomicBoolean loadFinished = new AtomicBoolean(false);

	public static void setApplicationContext(ApplicationContext applicationContext) {
		if(InstanceFactory.applicationContext != null)return;
		InstanceFactory.applicationContext = applicationContext;
		initialized.set(true);
		//
		EnvironmentHelper.init(applicationContext);
	}



	public static void loadFinished(){
		loadFinished.set(true);
	}

	public static boolean isLoadfinished(){
		return loadFinished.get();
	}

	
	public static ApplicationContext getContext(){
		return applicationContext;
	}
	
	public static DefaultListableBeanFactory getBeanFactory(){
        return (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
    }
	
	@SuppressWarnings("unchecked")
	public static <T> T getInstance(Class<T> beanClass) {
		String[] beanNames = applicationContext.getBeanNamesForType(beanClass);
		if (beanNames.length == 0) {
			return null;
		}
		return (T) applicationContext.getBean(beanNames[0]);
	}

	public static <T> T getInstance(Class<T> beanClass, String beanName) {
		return (T) applicationContext.getBean(beanName, beanClass);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getByBeanName(String beanName) {
		return (T) applicationContext.getBean(beanName);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getInstance(String beanName) {
		return (T) applicationContext.getBean(beanName);
	}

	@Deprecated
	public static <T> int getInterfaceCount(Class<T> beanClass) {
		return getBeanCountOfType(beanClass);
	}

	@Deprecated
	public static <T> Map<String, T> getInterfaces(Class<T> beanClass) {
		return getBeansOfType(beanClass);
	}
	
	public static <T> int getBeanCountOfType(Class<T> beanClass) {
		return applicationContext.getBeanNamesForType(beanClass).length;
	}

	public static <T> Map<String, T> getBeansOfType(Class<T> beanClass) {
		return applicationContext.getBeansOfType(beanClass);
	}
	
	/**
	 * 这是一个阻塞方法，直到context初始化完成
	 */
	public synchronized static void waitUtilInitialized(){
		if(initialized.get())return;
		while(true){
			if(initialized.get())break;
			try {Thread.sleep(1000);} catch (Exception e) {}
			long waiting = System.currentTimeMillis() - timeStarting;
			if(waiting >60 * 1000)throw new RuntimeException("Spring Initialize failture");
			System.out.println("Spring Initializing >>>>>"+waiting + " s");
		}
	}

	public static boolean isInitialized() {
		return initialized.get();
	}
	
	

}
