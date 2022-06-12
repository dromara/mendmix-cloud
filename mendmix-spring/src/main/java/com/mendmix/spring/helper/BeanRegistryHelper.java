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
package com.mendmix.spring.helper;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

public class BeanRegistryHelper {
	
	private static final Logger logger = LoggerFactory.getLogger("com.mendmix.spring");

	public static String register(ApplicationContext context,String beanName,Class<?> beanClass,List<BeanValue> constructorArgValues,Map<String, BeanValue> propertyPairs) {
		DefaultListableBeanFactory registry = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();
		return register(registry, beanName, beanClass, constructorArgValues, propertyPairs);
	}
		
	public static String register(BeanDefinitionRegistry registry,String beanName,Class<?> beanClass,List<BeanValue> constructorArgValues,Map<String, BeanValue> propertyPairs) {
		
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
		
		if(constructorArgValues != null) {
			for (BeanValue arg : constructorArgValues) {
				if(arg.reference) {
					beanDefinitionBuilder.addConstructorArgReference(arg.value.toString());
				}else {
					beanDefinitionBuilder.addConstructorArgValue(arg.value);
				}
				
			}
		}

		if(propertyPairs != null) {
			propertyPairs.forEach( (k,v) -> {
				if(v.reference) {
					beanDefinitionBuilder.addPropertyReference(k, v.value.toString());
				}else {
					beanDefinitionBuilder.addPropertyValue(k, v.value);
				}
			} );
		}

		registry.registerBeanDefinition(beanName, beanDefinitionBuilder.getRawBeanDefinition());
		
		//bean注册的holer类.
       // BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinitionBuilder.getRawBeanDefinition(), beanName);
        //使用bean注册工具类进行注册.
        //BeanDefinitionReaderUtils.registerBeanDefinition(beanDefinitionHolder, registry);
		
		logger.info("MENDMIX-TRACE-LOGGGING-->> register Bean[{}] Finished -> className:{}",beanName,beanClass.getName());
		
		return beanName;
		
	}
	
	public static class BeanValue {
		public Object value;
		public boolean reference;
		
		public BeanValue(Object value) {
			this.value = value;
		}
		
		public BeanValue(Object value, boolean reference) {
			super();
			this.value = value;
			this.reference = reference;
		}

		@Override
		public String toString() {
			return "BeanValue [value=" + value + ", reference=" + reference + "]";
		}

	}
}
