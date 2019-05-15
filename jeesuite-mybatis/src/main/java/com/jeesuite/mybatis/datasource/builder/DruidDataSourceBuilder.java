package com.jeesuite.mybatis.datasource.builder;

import java.util.Properties;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import com.alibaba.druid.pool.DruidDataSource;

public class DruidDataSourceBuilder {

	public static BeanDefinitionBuilder builder(Properties props){
		
		BeanDefinitionBuilder beanDefinitionBuilder =  BeanDefinitionBuilder.genericBeanDefinition(DruidDataSource.class);
		
		if(props.containsKey("name"))beanDefinitionBuilder.addPropertyValue("name", props.getProperty("name"));
    	beanDefinitionBuilder.addPropertyValue("driverClassName", props.getProperty("driverClassName"));
    	beanDefinitionBuilder.addPropertyValue("url", props.getProperty("url"));
    	beanDefinitionBuilder.addPropertyValue("username", props.getProperty("username"));
    	beanDefinitionBuilder.addPropertyValue("password", props.getProperty("password"));
    	beanDefinitionBuilder.addPropertyValue("testWhileIdle", Boolean.parseBoolean(props.getProperty("testWhileIdle","true")));
    	beanDefinitionBuilder.addPropertyValue("validationQuery", props.getProperty("validationQuery","SELECT 'x'"));
    	beanDefinitionBuilder.addPropertyValue("maxActive", Integer.parseInt(props.getProperty("maxActive","10")));
    	beanDefinitionBuilder.addPropertyValue("initialSize", Integer.parseInt(props.getProperty("initialSize","1")));
    	beanDefinitionBuilder.addPropertyValue("minIdle", Integer.parseInt(props.getProperty("minIdle","1")));
    	beanDefinitionBuilder.addPropertyValue("maxWait", Long.parseLong(props.getProperty("maxWait","10000")));
    	beanDefinitionBuilder.addPropertyValue("minEvictableIdleTimeMillis", Long.parseLong(props.getProperty("minEvictableIdleTimeMillis","60000")));
    	beanDefinitionBuilder.addPropertyValue("timeBetweenEvictionRunsMillis", Long.parseLong(props.getProperty("timeBetweenEvictionRunsMillis","60000")));
    	beanDefinitionBuilder.addPropertyValue("testOnBorrow", Boolean.parseBoolean(props.getProperty("testOnBorrow","true")));
    	beanDefinitionBuilder.addPropertyValue("testOnReturn", Boolean.parseBoolean(props.getProperty("testOnReturn","false")));

    	return beanDefinitionBuilder;
        
	}
}
