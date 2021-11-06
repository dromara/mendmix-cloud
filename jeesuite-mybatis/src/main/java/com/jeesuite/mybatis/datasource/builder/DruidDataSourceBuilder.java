package com.jeesuite.mybatis.datasource.builder;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.jeesuite.common.util.BeanUtils;
import com.jeesuite.mybatis.datasource.DataSourceConfig;

public class DruidDataSourceBuilder {

	public static DataSource builder(DataSourceConfig config) throws SQLException {

		DruidDataSource dataSource = new DruidDataSource();
		BeanUtils.copy(config, dataSource);
	    //
		dataSource.init();

		return dataSource;

	}

}
