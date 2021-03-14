package com.jeesuite.mybatis.core;

import com.jeesuite.mybatis.plugin.InvocationVals;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;

/**
 * mybatis插件拦截处理器接口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
public interface InterceptorHandler {
	
	void start(JeesuiteMybatisInterceptor context);
	
	void close();

	Object onInterceptor(InvocationVals invocationVal) throws Throwable;
	
	void onFinished(InvocationVals invocationVal,Object result);
	
	int interceptorOrder();
	
	
}
