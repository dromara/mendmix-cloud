package com.jeesuite.mybatis.plugin.shard;

import org.apache.ibatis.plugin.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.core.InterceptorType;

/**
 * 分库自动路由处理
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
public class TableRouteHandler implements InterceptorHandler,InitializingBean {


	protected static final Logger logger = LoggerFactory.getLogger(TableRouteHandler.class);
	
	
	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object onInterceptor(Invocation invocation) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onFinished(Invocation invocation, Object result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public InterceptorType getInterceptorType() {
		// TODO Auto-generated method stub
		return null;
	}
} 

