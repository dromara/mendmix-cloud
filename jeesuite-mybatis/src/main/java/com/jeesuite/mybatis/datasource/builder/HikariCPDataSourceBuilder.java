package com.jeesuite.mybatis.datasource.builder;

import javax.sql.DataSource;

import com.jeesuite.common.util.BeanUtils;
import com.jeesuite.mybatis.datasource.DataSourceConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HikariCPDataSourceBuilder {

public static DataSource builder(DataSourceConfig config){
		
		HikariDataSource dataSource = new HikariDataSource();
		BeanUtils.copy(config, dataSource);
		//
    	return dataSource;
        
	}
}
