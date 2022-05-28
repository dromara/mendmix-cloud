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

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

import com.mendmix.spring.helper.EnvironmentHelper;

public class InstanceFactory {

	private static ApplicationContext applicationContext;
	static{ System.setProperty("framework.website", "www.mendmix.com");}

	/**
	 * 设置实例提供者。
	 * @param provider 一个实例提供者的实例。
	 */
	public static void setApplicationContext(ApplicationContext applicationContext) {
		if(InstanceFactory.applicationContext != null)return;
		InstanceFactory.applicationContext = applicationContext;
		EnvironmentHelper.init(applicationContext);
	}

	/**
	 * 获取指定类型的对象实例。如果IoC容器没配置好或者IoC容器中找不到该类型的实例则抛出异常。
	 * 
	 * @param <T> 对象的类型
	 * @param beanClass 对象的类
	 * @return 类型为T的对象实例
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getInstance(Class<T> beanClass) {
		String[] beanNames = applicationContext.getBeanNamesForType(beanClass);
		if (beanNames.length == 0) {
			return null;
		}
		return (T) applicationContext.getBean(beanNames[0]);
	}

	/**
	 * 获取指定类型的对象实例。如果IoC容器没配置好或者IoC容器中找不到该实例则抛出异常。
	 * 
	 * @param <T> 对象的类型
	 * @param beanName 实现类在容器中配置的名字
	 * @param beanClass 对象的类
	 * @return 类型为T的对象实例
	 */
	public static <T> T getInstance(Class<T> beanClass, String beanName) {
		return (T) applicationContext.getBean(beanClass, beanName);
	}

	/**
	 * 获取指定类型的对象实例
	 * @param <T> 对象的类型
	 * @param beanName 实现类在容器中配置的名字
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getInstance(String beanName) {
		return (T) applicationContext.getBean(beanName);
	}
	
	public static ApplicationContext getContext(){
		return applicationContext;
	}
	
	public static <T> Map<String,  T> getBeansOfType(Class<T> clazz){
		return applicationContext.getBeansOfType(clazz);
	}
	
	public static DefaultListableBeanFactory getBeanFactory(){
        return (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
    }

}
