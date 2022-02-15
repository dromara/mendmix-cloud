package com.jeesuite.spring;

import java.util.Map;

import org.springframework.context.ApplicationContext;

import com.jeesuite.spring.helper.EnvironmentHelper;

public class InstanceFactory {

	private static ApplicationContext applicationContext;
	static{ System.setProperty("framework.website", "www.jeesuite.com");}

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

}
