package com.jeesuite.springboot.autoconfigure;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jeesuite.mybatis.datasource.DataSourceConfig;
import com.jeesuite.mybatis.datasource.MultiRouteDataSource;
import com.jeesuite.spring.InstanceFactory;


@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@ConditionalOnClass(name = {"org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"})
@ConditionalOnProperty("master.db.url")
public class DefaultDataSourceConfiguration implements ApplicationContextAware{

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		InstanceFactory.setApplicationContext(context);
	}
	
	@Bean("dataSource")
    public DataSource defaultDataSource(){
    	return new MultiRouteDataSource(DataSourceConfig.DEFAULT_GROUP_NAME);
    }
}
