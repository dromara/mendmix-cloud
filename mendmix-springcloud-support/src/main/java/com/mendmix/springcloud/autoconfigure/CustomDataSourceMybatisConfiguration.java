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
package com.mendmix.springcloud.autoconfigure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import com.mendmix.common.util.ResourceUtils;
import com.mendmix.mybatis.datasource.DataSourceConfig;
import com.mendmix.mybatis.datasource.DataSoureConfigHolder;
import com.mendmix.mybatis.datasource.MultiRouteDataSource;
import com.mendmix.mybatis.spring.SqlSessionFactoryBean;
import com.mendmix.spring.InstanceFactory;
import com.mendmix.spring.helper.BeanRegistryHelper;
import com.mendmix.spring.helper.BeanRegistryHelper.BeanValue;

/**
 * 自定义数据源加载
 * 
 * <br>
 * Class Name   : CustomDataSourceMybatisConfiguration
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2021-03-18
 */
@Configuration
@ConditionalOnClass({MultiRouteDataSource.class})
@ConditionalOnMissingClass({"org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"})
public class CustomDataSourceMybatisConfiguration implements ApplicationContextAware, BeanDefinitionRegistryPostProcessor {

	private static final Logger logger = LoggerFactory.getLogger("com.mendmix.springcloud.starter");

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		InstanceFactory.setApplicationContext(context);
	}
	
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {	
	}



	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		List<String> groups = DataSoureConfigHolder.getGroups();
		for (String group : groups) {
			registerGroupMybatisBean(registry,group);
			logger.info(">> registerGroupMybatisBean Finished -> group:{}",group);
		}
	}

	private void registerGroupMybatisBean(BeanDefinitionRegistry registry,String group) {

		String propKeyPrefix = "";
		if(!DataSourceConfig.DEFAULT_GROUP_NAME.equals(group)) {
			propKeyPrefix = group + ".";
		}
	
		List<BeanValue> argValues = new ArrayList<>();
		Map<String, BeanValue> propertyPairs = new HashMap<>();
		//----
		String dataSourceBeanName = group + "Datasource";
		Class<?> dataSourceClass = com.mendmix.mybatis.datasource.MultiRouteDataSource.class;
		argValues.add(new BeanValue(group));
		BeanRegistryHelper.register(registry, dataSourceBeanName, dataSourceClass, argValues, propertyPairs);
		//----
		Class<?> transactionManagerClass = org.springframework.jdbc.datasource.DataSourceTransactionManager.class;
		String transactionManagerBeanName = group +  "TransactionManager";
		
		argValues.clear();
		propertyPairs.clear();
		propertyPairs.put("dataSource", new BeanValue(dataSourceBeanName, true));
		BeanRegistryHelper.register(registry, transactionManagerBeanName, transactionManagerClass, argValues, propertyPairs);
		
		//----
		Class<?> transactionTemplateClass = org.springframework.transaction.support.TransactionTemplate.class;
		String transactionTemplateBeanName = group +  "TransactionTemplate";
		
		argValues.clear();
		propertyPairs.clear();
		propertyPairs.put("transactionManager", new BeanValue(transactionManagerBeanName, true));
		BeanRegistryHelper.register(registry, transactionTemplateBeanName, transactionTemplateClass, argValues, propertyPairs);
		
        //----
		String sessionFactoryBeanName = group + "SqlSessionFactoryBean";
		Class<?> sessionFactoryClass = SqlSessionFactoryBean.class;
		
		argValues.clear();
		propertyPairs.clear();
		
		propertyPairs.put("groupName", new BeanValue(group));
		String value = ResourceUtils.getProperty(propKeyPrefix + "mybatis.mapper-locations");
		propertyPairs.put("mapperLocations", new BeanValue(value));
		value = ResourceUtils.getProperty(propKeyPrefix + "mybatis.type-aliases-package");
		propertyPairs.put("typeAliasesPackage", new BeanValue(value));
		value = ResourceUtils.getProperty(propKeyPrefix + "mybatis.type-handlers-package");
		if(value != null)propertyPairs.put("typeHandlersPackage", new BeanValue(value));
		propertyPairs.put("dataSource", new BeanValue(dataSourceBeanName, true));
		BeanRegistryHelper.register(registry, sessionFactoryBeanName, sessionFactoryClass, argValues, propertyPairs);
		
		//----
		String mapperConfigurerBeanName = group + "MapperScannerConfigurer";
		Class<?> mapperConfigurerClass = null;
		if("tkMapper".equals(ResourceUtils.getProperty("mendmix.mybatis.crudDriver"))) {
			try {mapperConfigurerClass = Class.forName("tk.mybatis.spring.mapper.MapperScannerConfigurer");} catch (ClassNotFoundException e) {}
		}
		if(mapperConfigurerClass == null) {
			mapperConfigurerClass = org.mybatis.spring.mapper.MapperScannerConfigurer.class;
		}
		argValues.clear();
		propertyPairs.clear();
		propertyPairs.put("sqlSessionFactoryBeanName", new BeanValue(sessionFactoryBeanName));
		value = ResourceUtils.getProperty(propKeyPrefix + "mybatis.mapper-package");
		propertyPairs.put("basePackage", new BeanValue(value));
		BeanRegistryHelper.register(registry, mapperConfigurerBeanName, mapperConfigurerClass, argValues, propertyPairs);
	}

}
