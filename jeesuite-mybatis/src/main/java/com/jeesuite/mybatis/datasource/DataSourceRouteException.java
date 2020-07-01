/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.mybatis.datasource;

import org.springframework.dao.DataAccessException;

/**
 * 
 * <br>
 * Class Name   : DataSourceRouteException
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年7月1日
 */
public class DataSourceRouteException extends DataAccessException {

	private static final long serialVersionUID = -3481243690930570853L;

	/**
	 * @param msg
	 */
	public DataSourceRouteException(String msg) {
		super(msg);
	}

}
