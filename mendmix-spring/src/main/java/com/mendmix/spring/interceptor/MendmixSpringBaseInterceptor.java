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
package com.mendmix.spring.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;

import com.mendmix.spring.InterceptorHanlder;

/**
 * 
 * <br>
 * Class Name   : JeesuiteSpringBaseInterceptor
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年10月31日
 */
public abstract class MendmixSpringBaseInterceptor {
	
	private static List<InterceptorHanlder> handlers  =  new ArrayList<>();
	static {
		try {
			Class<?> clazz = Class.forName("com.mendmix.mybatis.spring.MyBatisInterceptorHanlder");
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
