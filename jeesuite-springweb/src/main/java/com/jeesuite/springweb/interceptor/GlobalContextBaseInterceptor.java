/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.springweb.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;

import com.jeesuite.spring.InterceptorHanlder;

/**
 * 
 * <br>
 * Class Name   : GlobalContextBaseInterceptor
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年10月31日
 */
public abstract class GlobalContextBaseInterceptor {
	
	private static List<InterceptorHanlder> handlers  =  new ArrayList<>();
	static {
		try {
			Class<?> clazz = Class.forName("com.jeesuite.mybatis.spring.MyBatisInterceptorHanlder");
			InterceptorHanlder  hanlder = (InterceptorHanlder) clazz.newInstance();
			handlers.add(hanlder);
		} catch (Exception e) {}
	}
	
	public static void registerHandler(InterceptorHanlder hanlder) {
		handlers.add(hanlder);
	}

	public abstract void pointcut();
	
	@Around("pointcut()")
	public Object around(ProceedingJoinPoint pjp) throws Throwable {

		Method method = ((MethodSignature)pjp.getSignature()).getMethod();  
		Object[] args = pjp.getArgs();
		try {
			for (InterceptorHanlder hanlder : handlers) {
				hanlder.preHandler(method, args);
			}
			
			return pjp.proceed();
		} catch (Exception e) {
			for (InterceptorHanlder hanlder : handlers) {
				hanlder.postHandler(method, args, e);
			}
			throw e;
		}finally {
			//由于一些变量是整个请求都会用到 ，如果方法调用另外方法出进入多次，所以这里不全局清理,由handler各自清理
			//ThreadLocalContext.unset();
			for (InterceptorHanlder hanlder : handlers) {
				hanlder.destory();
			}
		}
		
	}
}
