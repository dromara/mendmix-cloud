package com.jeesuite.spring;

import java.lang.reflect.Method;

/**
 * 全局拦截器处理handler
 * 
 * <br>
 * Class Name   : InterceptorHanlder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Oct 31, 2020
 */
public interface InterceptorHanlder {

	void preHandler(Method method,Object[] args);
	
	void postHandler(Method method,Object result,Exception ex);
	
	void destory();
}
