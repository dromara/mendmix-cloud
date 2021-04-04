package com.jeesuite.mybatis.spring;

import java.lang.reflect.Method;

import org.springframework.transaction.annotation.Transactional;

import com.jeesuite.mybatis.MybatisRuntimeContext;
import com.jeesuite.mybatis.plugin.dataprofile.annotation.DataProfileIgnore;
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
		//多个方法层级调用 ，以最外层方法定义为准
		if(!MybatisRuntimeContext.isTransactionalOn() && method.isAnnotationPresent(Transactional.class)) {
			MybatisRuntimeContext.setTransactionalMode(true);
		}
		
		if(!MybatisRuntimeContext.isForceUseMaster() && method.isAnnotationPresent(UseMaster.class)){				
			MybatisRuntimeContext.forceMaster();
		}
		
		if(!MybatisRuntimeContext.isDataProfileIgnore() && method.isAnnotationPresent(DataProfileIgnore.class)){				
			MybatisRuntimeContext.dataProfileIgnore();
		}
	}

	@Override
	public void postHandler(Method method, Object[] args, Exception ex) {}

	@Override
	public void destory() {
		MybatisRuntimeContext.unsetEveryTime();
	}

}
