/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.spring.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;

import org.dromara.mendmix.spring.InterceptorHanlder;

/**
 * 
 * <br>
 * Class Name   : MendmixSpringBaseInterceptor
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年10月31日
 */
public abstract class MendmixSpringBaseInterceptor {
	
	private static List<InterceptorHanlder> handlers  =  new ArrayList<>();
	static {
		try {
			Class<?> clazz = Class.forName("org.dromara.mendmix.mybatis.spring.MyBatisInterceptorHanlder");
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

		Exception ex = null;
		Method method = ((MethodSignature)pjp.getSignature()).getMethod();  
		Object[] args = pjp.getArgs();
		
		Object result = null;
		try {
			for (InterceptorHanlder hanlder : handlers) {
				hanlder.preHandler(method, args);
			}
			return (result = pjp.proceed());
		} catch (Exception e) {
			ex = e;
			throw e;
		}finally {
			for (InterceptorHanlder hanlder : handlers) {
				hanlder.postHandler(result, ex);
			}
		}
		
	}
}
