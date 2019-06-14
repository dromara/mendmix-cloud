package com.jeesuite.springboot.starter.mybatis;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jeesuite.mybatis.datasource.MutiRouteDataSource;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;


@Configuration
@ConditionalOnClass(MutiRouteDataSource.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class JeesuiteDataSourceConfiguration implements ApplicationContextAware {

	@Bean("dataSource")
    public DataSource dataSourceBean(){
    	return new MutiRouteDataSource();
    }

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		InstanceFactory.setInstanceProvider(new SpringInstanceProvider(applicationContext));
	}

}
