package com.jeesuite.springboot.starter.mybatis;

import javax.sql.DataSource;

import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jeesuite.mybatis.datasource.DataSourceConfig;
import com.jeesuite.mybatis.datasource.MultiRouteDataSource;


@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@ConditionalOnClass(MybatisAutoConfiguration.class)
public class DefaultDataSourceConfiguration {

	@Bean("dataSource")
    public DataSource defaultDataSource(){
    	return new MultiRouteDataSource(DataSourceConfig.DEFAULT_GROUP_NAME);
    }
}
