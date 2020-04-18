package com.jeesuite.mybatis.spring;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.transaction.annotation.Transactional;

import com.jeesuite.mybatis.MybatisRuntimeContext;
import com.jeesuite.mybatis.datasource.MuitDataSourceManager;
import com.jeesuite.mybatis.plugin.cache.CacheHandler;
import com.jeesuite.mybatis.plugin.rwseparate.UseMaster;

/**
 * AOP拦截器基类
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年5月1日
 */
public abstract class MybatisPluginBaseSpringInterceptor {

	public abstract void pointcut();

	@Around("pointcut()")
	public Object around(ProceedingJoinPoint pjp) throws Throwable {
		try {
			MethodSignature methodSignature = (MethodSignature)pjp.getSignature();    
			Method method = methodSignature.getMethod();  
			
			if(method.isAnnotationPresent(Transactional.class)){
				MybatisRuntimeContext.setTransactionalMode(true);
			}
			if(method.isAnnotationPresent(UseMaster.class)){				
				MuitDataSourceManager.get().forceMaster();
			}
			return pjp.proceed();
		} catch (Exception e) {
			CacheHandler.rollbackCache();
			throw e;
		}finally {
			MybatisRuntimeContext.unset();
		}
		
	}
}
