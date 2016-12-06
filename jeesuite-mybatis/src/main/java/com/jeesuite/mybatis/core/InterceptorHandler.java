package com.jeesuite.mybatis.core;

import org.apache.ibatis.plugin.Invocation;

import com.jeesuite.mybatis.plugin.JeesuiteMybatisPluginContext;

/**
 * mybatis插件拦截处理器接口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
public interface InterceptorHandler {
	
	void start(JeesuiteMybatisPluginContext context);
	
	void close();

	Object onInterceptor(Invocation invocation) throws Throwable;
	
	void onFinished(Invocation invocation,Object result);
	
	InterceptorType getInterceptorType(); 
}
