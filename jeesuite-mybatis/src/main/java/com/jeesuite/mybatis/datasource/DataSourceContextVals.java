package com.jeesuite.mybatis.datasource;

import com.jeesuite.mybatis.MybatisRuntimeContext;

/**
 * 
 * <br>
 * Class Name   : DataSourceContextVals
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年4月20日
 */
public class DataSourceContextVals {
	public String tenantId = MybatisRuntimeContext.getTenantId();
	public Boolean master; //
}
