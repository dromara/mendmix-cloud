/**
 * 
 */
package com.jeesuite.test.sch;

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
	@Pointcut("execution(* com.jeesuite.test.sch..*.*(..))")  
    public void pointcut(){}

	 @Around("pointcut()") 
	 public Object around(ProceedingJoinPoint pjp) throws Throwable{
		 System.out.println("并没什么卵用，只是测试cglib生成代理类");
		 return pjp.proceed();
	 }  

}
