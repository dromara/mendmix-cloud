/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.mybatis.plugin.rwseparate;

import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.mybatis.core.InterceptorHandler;
import com.mendmix.mybatis.plugin.InvocationVals;
import com.mendmix.mybatis.plugin.JeesuiteMybatisInterceptor;


/**
 * 读写分离自动路由处理
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
public class RwRouteHandler implements InterceptorHandler {

	protected static final Logger logger = LoggerFactory.getLogger(RwRouteHandler.class);

	public static final String NAME = "rwRoute";
	
	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
		
		MappedStatement ms = invocation.getMappedStatement();
		//已指定强制使用
		if(MybatisRuntimeContext.isRwRouteAssigned()){
			logger.debug("RwRouteAssigned for:{},useMaster:{}",ms.getId(),MybatisRuntimeContext.isUseMaster());
			return null;
		}
		
		//读方法
		if(ms.getSqlCommandType().equals(SqlCommandType.SELECT)){
			//!selectKey 为自增id查询主键(SELECT LAST_INSERT_ID() )方法，使用主库
			if(!ms.getId().contains(SelectKeyGenerator.SELECT_KEY_SUFFIX)){				
				MybatisRuntimeContext.useSlave();
				logger.debug("Method[{} use Slave Strategy..",ms.getId());
			}
		}else{
			logger.debug("Method[{}] use Master Strategy..",ms.getId());
			MybatisRuntimeContext.userMaster();
		}
		
		return null;
	}

	@Override
	public void onFinished(InvocationVals invocation,Object result) {}

	@Override
	public void start(JeesuiteMybatisInterceptor context) {}


	@Override
	public void close() {}

	@Override
	public int interceptorOrder() {
		return 1;
	}
}
