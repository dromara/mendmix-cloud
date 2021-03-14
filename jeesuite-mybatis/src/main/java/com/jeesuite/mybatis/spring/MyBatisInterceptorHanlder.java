package com.jeesuite.mybatis.spring;

import java.lang.reflect.Method;

import org.springframework.transaction.annotation.Transactional;

import com.jeesuite.mybatis.MybatisRuntimeContext;
import com.jeesuite.mybatis.plugin.rwseparate.UseMaster;
import com.jeesuite.spring.InterceptorHanlder;

/**
 * 
 * 
 * <br>
 * Class Name   : MyBatisInterceptorHanlder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Oct 31, 2020
 */
public class MyBatisInterceptorHanlder implements InterceptorHanlder {

	@Override
	public void preHandler(Method method, Object[] args) {
		boolean first = MybatisRuntimeContext.isEmpty();
		if(first){				
			if(method.isAnnotationPresent(Transactional.class)){
				MybatisRuntimeContext.setTransactionalMode(true);
			}
			if(method.isAnnotationPresent(UseMaster.class)){				
				MybatisRuntimeContext.forceMaster();
			}
		}
	}

	@Override
	public void postHandler(Method method, Object[] args, Exception ex) {}

	@Override
	public void destory() {
		MybatisRuntimeContext.unset();
	}

}
