package com.jeesuite.mybatis.datasource.builder;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import com.alibaba.druid.pool.DruidDataSource;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.SimpleCryptUtils;

public class DruidDataSourceBuilder {

	public static BeanDefinitionBuilder builder(Properties props){
		
		BeanDefinitionBuilder beanDefinitionBuilder =  BeanDefinitionBuilder.genericBeanDefinition(DruidDataSource.class);
		
		String cryptKey = props.getProperty("config.cryptKey");
		if(props.containsKey("name"))beanDefinitionBuilder.addPropertyValue("name", props.getProperty("name"));
    	beanDefinitionBuilder.addPropertyValue("driverClassName", props.getProperty("driverClassName"));
    	beanDefinitionBuilder.addPropertyValue("url", props.getProperty("url"));
    	beanDefinitionBuilder.addPropertyValue("username", props.getProperty("username"));
    	
    	String password = props.getProperty("password");
    	if(StringUtils.isNotBlank(cryptKey) && !ResourceUtils.NULL_VALUE_PLACEHOLDER.equals(cryptKey)){
    		try {
    			password = SimpleCryptUtils.decrypt(cryptKey, password);
			} catch (Exception e) {
				System.err.println(">> find config[db.config.cryptKey],but decrypt error ,use orign password");
			}
    	}
    	beanDefinitionBuilder.addPropertyValue("password", password);
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
