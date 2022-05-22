/*
 * Copyright 2016-2020 www.jeesuite.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
