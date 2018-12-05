/*
 * Copyright 2016-2018 www.jeesuite.com.
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
package com.jeesuite.mybatis.plugin.security;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月11日
 */
public class SensitiveOperProtectHandler implements InterceptorHandler{

	@Override
	public void start(JeesuiteMybatisInterceptor context) {}

	@Override
	public void close() {}

	@Override
	public Object onInterceptor(Invocation invocation) throws Throwable {
		Object[] objects = invocation.getArgs();
		MappedStatement ms = (MappedStatement) objects[0];
		if(ms.getSqlCommandType().equals(SqlCommandType.DELETE)){
			throw new JeesuiteBaseException(4003, "当前已开启敏感操作保护");
		}
		return null;
	}

	@Override
	public void onFinished(Invocation invocation, Object result) {
		
	}

	@Override
	public int interceptorOrder() {
		return 1;
	}

}
