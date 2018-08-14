package com.jeesuite.mybatis.datasource.builder;

import java.util.Properties;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import com.zaxxer.hikari.HikariDataSource;

public class HikariCPDataSourceBuilder {

	public static BeanDefinitionBuilder builder(Properties props){
		
		BeanDefinitionBuilder beanDefinitionBuilder =  BeanDefinitionBuilder.genericBeanDefinition(HikariDataSource.class);
		if(props.containsKey("driverClassName"))beanDefinitionBuilder.addPropertyValue("driverClassName", props.getProperty("driverClassName"));
    	beanDefinitionBuilder.addPropertyValue("jdbcUrl", props.getProperty("jdbcUrl"));
    	beanDefinitionBuilder.addPropertyValue("username", props.getProperty("username"));
    	beanDefinitionBuilder.addPropertyValue("password", props.getProperty("password"));
    	beanDefinitionBuilder.addPropertyValue("connectionTestQuery", props.getProperty("connectionTestQuery","SELECT 'x'"));
    	beanDefinitionBuilder.addPropertyValue("connectionTimeout", Long.parseLong(props.getProperty("connectionTimeout","10000")));
    	beanDefinitionBuilder.addPropertyValue("idleTimeout", Long.parseLong(props.getProperty("idleTimeout","600000")));
    	beanDefinitionBuilder.addPropertyValue("maximumPoolSize", Integer.parseInt(props.getProperty("maximumPoolSize","10")));
    	beanDefinitionBuilder.addPropertyValue("maxLifetime", Long.parseLong(props.getProperty("maxLifetime","900000")));
    	return beanDefinitionBuilder;
        
	}
}
