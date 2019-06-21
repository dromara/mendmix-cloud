package com.jeesuite.springboot.starter.mybatis;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jeesuite.mybatis.datasource.MutiRouteDataSource;


@Configuration
@ConditionalOnClass(MutiRouteDataSource.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class JeesuiteDataSourceConfiguration {

	@Bean("dataSource")
    public DataSource dataSourceBean(){
    	return new MutiRouteDataSource();
    }
}
