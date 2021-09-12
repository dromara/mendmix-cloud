package com.jeesuite.springboot.starter.mybatis;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.mybatis.datasource.DataSoureConfigHolder;
import com.jeesuite.mybatis.spring.SqlSessionFactoryBean;
import com.jeesuite.spring.helper.BeanRegistryHelper;
import com.jeesuite.spring.helper.BeanRegistryHelper.BeanValue;

/**
 * 自定义数据源加载
 * 
 * <br>
 * Class Name   : CustomDataSourceMybatisConfiguration
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Sep 12, 2021
 */
@Configuration
@ConditionalOnMissingClass({"org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"})
public class CustomDataSourceMybatisConfiguration implements ApplicationContextAware, BeanDefinitionRegistryPostProcessor {

	private static final Logger logger = LoggerFactory.getLogger("com.jeesuite.springboot.starter");
	private ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
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

		String propKeyPrefix = group + ".";
	
		List<BeanValue> argValues = new ArrayList<>();
		Map<String, BeanValue> propertyPairs = new HashMap<>();
		//----
		String dataSourceBeanName = group + "Datasource";
		Class<?> dataSourceClass = com.jeesuite.mybatis.datasource.MultiRouteDataSource.class;
		argValues.add(new BeanValue(group));
		BeanRegistryHelper.register(registry, dataSourceBeanName, dataSourceClass, argValues, propertyPairs);
		context.getBean(dataSourceBeanName);
		//----
		Class<?> transactionManagerClass = org.springframework.jdbc.datasource.DataSourceTransactionManager.class;
		String transactionManagerBeanName = group +  "TransactionManager";
		
		argValues.clear();
		propertyPairs.clear();
		propertyPairs.put("dataSource", new BeanValue(dataSourceBeanName, true));
		BeanRegistryHelper.register(registry, transactionManagerBeanName, transactionManagerClass, argValues, propertyPairs);
		context.getBean(transactionManagerBeanName);
		
		//----
		Class<?> transactionTemplateClass = org.springframework.transaction.support.TransactionTemplate.class;
		String transactionTemplateBeanName = group +  "TransactionTemplate";
		
		argValues.clear();
		propertyPairs.clear();
		propertyPairs.put("transactionManager", new BeanValue(transactionManagerBeanName, true));
		BeanRegistryHelper.register(registry, transactionTemplateBeanName, transactionTemplateClass, argValues, propertyPairs);
		context.getBean(transactionTemplateBeanName);
		
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
		propertyPairs.put("dataSource", new BeanValue(dataSourceBeanName, true));
		BeanRegistryHelper.register(registry, sessionFactoryBeanName, sessionFactoryClass, argValues, propertyPairs);
		// 触发bean实例化
		context.getBean(sessionFactoryBeanName);
		//----
		String mapperConfigurerBeanName = group + "MapperScannerConfigurer";
		Class<?> mapperConfigurerClass = org.mybatis.spring.mapper.MapperScannerConfigurer.class;
		argValues.clear();
		propertyPairs.clear();
		propertyPairs.put("sqlSessionFactoryBeanName", new BeanValue(sessionFactoryBeanName));
		value = ResourceUtils.getProperty(propKeyPrefix + "mybatis.mapper-package");

		propertyPairs.put("basePackage", new BeanValue(value));
		BeanRegistryHelper.register(registry, mapperConfigurerBeanName, mapperConfigurerClass, argValues, propertyPairs);
		context.getBean(mapperConfigurerBeanName);
	}

}
