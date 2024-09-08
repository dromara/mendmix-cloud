/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.mybatis.autoconfigure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.mybatis.datasource.DataSourceConfig;
import org.dromara.mendmix.mybatis.datasource.DataSoureConfigHolder;
import org.dromara.mendmix.mybatis.datasource.DynamicMultiDataSource;
import org.dromara.mendmix.mybatis.spring.SqlSessionFactoryBean;
import org.dromara.mendmix.spring.InstanceFactory;
import org.dromara.mendmix.spring.helper.BeanRegistryHelper;
import org.dromara.mendmix.spring.helper.BeanRegistryHelper.BeanValue;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;


@Configuration
@ConditionalOnMissingClass({"org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"})
public class MybatisConfiguration implements ApplicationContextAware, BeanDefinitionRegistryPostProcessor {

	private static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix");

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

		List<BeanValue> argValues = new ArrayList<>();
		Map<String, BeanValue> propertyPairs = new HashMap<>();
		//----
		String dataSourceBeanName = group + "DataSource";
		Class<?> dataSourceClass = DynamicMultiDataSource.class;
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
		propertyPairs.put("configLocation", new BeanValue("classpath:mybatis-configuration.xml"));
		String value = getGroupConfig(group,"mybatis.mapper-locations");
		if(value.contains(",")) {
			String[] values = StringUtils.split(value, ",");
			propertyPairs.put("mapperLocations", new BeanValue(values));
		}else {			
			propertyPairs.put("mapperLocations", new BeanValue(value));
		}
		value = getGroupConfig(group,"mybatis.type-aliases-package");
		propertyPairs.put("typeAliasesPackage", new BeanValue(value));
		value = getGroupConfig(group,"mybatis.type-handlers-package");
		if(value != null) {
			propertyPairs.put("typeHandlersPackage", new BeanValue(value));
		}
		propertyPairs.put("dataSource", new BeanValue(dataSourceBeanName, true));
		//
		Properties properties = buildConfigurationProperties(group);
		if(properties != null) {
			propertyPairs.put("configurationProperties", new BeanValue(properties, false));
		}
		BeanRegistryHelper.register(registry, sessionFactoryBeanName, sessionFactoryClass, argValues, propertyPairs);
		
		//----
		String mapperConfigurerBeanName = group + "MapperScannerConfigurer";
		Class<?> mapperConfigurerClass;
		try {
			mapperConfigurerClass = Class.forName("tk.mybatis.spring.mapper.MapperScannerConfigurer");
		} catch (ClassNotFoundException e) {
			mapperConfigurerClass = org.mybatis.spring.mapper.MapperScannerConfigurer.class;
		}
		argValues.clear();
		propertyPairs.clear();
		propertyPairs.put("sqlSessionFactoryBeanName", new BeanValue(sessionFactoryBeanName));
		value = getGroupConfig(group,"mybatis.mapper-package");

		propertyPairs.put("basePackage", new BeanValue(value));
		BeanRegistryHelper.register(registry, mapperConfigurerBeanName, mapperConfigurerClass, argValues, propertyPairs);
		//
		Class<?> sqlSessionTemplateClass = SqlSessionTemplate.class;
		//sqlSessionFactory
        String sqlSessionTemplateBeanName = group +  "SqlSessionTemplate";
		
		argValues.clear();
		propertyPairs.clear();
		argValues.add(new BeanValue(sessionFactoryBeanName, true));
		BeanRegistryHelper.register(registry, sqlSessionTemplateBeanName, sqlSessionTemplateClass, argValues, propertyPairs);
        //
		Class<?>  jdbcTemplateClass = JdbcTemplate.class;
		String jdbcTemplateBeanName = group +  "JdbcTemplate";
		argValues.clear();
		propertyPairs.clear();
		argValues.add(new BeanValue(dataSourceBeanName, true));
		BeanRegistryHelper.register(registry, jdbcTemplateBeanName, jdbcTemplateClass, argValues, propertyPairs);
	}
	
	private String getGroupConfig(String group,String key) {
		if(DataSourceConfig.DEFAULT_GROUP_NAME.equals(group)) {
			return ResourceUtils.getProperty(key);
		}
		
		String groupPrefix = "group["+group+"].";
		String value = ResourceUtils.getProperty(groupPrefix + key);
		if(StringUtils.isBlank(value)) {
			groupPrefix = group + ".";
			value = ResourceUtils.getProperty(groupPrefix + key);
		}
		
		return value;
	}
	
	//mybatis.configuration.variables.demoDbName=`demo_db1`
	private Properties buildConfigurationProperties(String group) {
		Properties originProperties;
		if (DataSourceConfig.DEFAULT_GROUP_NAME.equals(group)) {
			originProperties = ResourceUtils.getAllProperties("mybatis.configuration.variables");
		} else {
			originProperties = ResourceUtils.getAllProperties("group[" + group + "].mybatis.configuration.variables");
			if (originProperties.isEmpty()) {
				originProperties = ResourceUtils.getAllProperties(group + ".mybatis.configuration.variables");
			}
		}
		if (originProperties.isEmpty())
			return originProperties;
		Properties formatProperties = new Properties();
		Iterator<Entry<Object, Object>> iterator = originProperties.entrySet().iterator();
		String spiltStr = ".variables.";
		while (iterator.hasNext()) {
			Entry<Object, Object> entry = iterator.next();
			if (entry.getValue() == null || StringUtils.isBlank(entry.getValue().toString())) {
				continue;
			}
			String key = StringUtils.splitByWholeSeparator(entry.getKey().toString(), spiltStr)[1];
			formatProperties.setProperty(key, entry.getValue().toString());
		}
		return formatProperties;
	}

}

