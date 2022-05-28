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
package com.mendmix.test.sch;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Aspect
@Service
@Order(0)
public class JobInterceptor{
	
	protected static final Logger logger = LoggerFactory.getLogger(JobInterceptor.class);

	//定义拦截切面
	@Pointcut("execution(* com.mendmix.test.sch..*.*(..))")  
    public void pointcut(){}

	 @Around("pointcut()") 
	 public Object around(ProceedingJoinPoint pjp) throws Throwable{
		 System.out.println("并没什么卵用，只是测试cglib生成代理类");
		 return pjp.proceed();
	 }  

}
