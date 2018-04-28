package com.jeesuite.mybatis.plugin.rwseparate;

import org.apache.logging.log4j.core.config.Order;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.jeesuite.mybatis.datasource.DataSourceContextHolder;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ForceMasterRouteInterceptor {

	@Pointcut("@annotation(com.jeesuite.mybatis.plugin.rwseparate.UseMaster) || @annotation(org.springframework.transaction.annotation.Transactional)")
	public void useMasterPointcut() {
	}

	@Around("useMasterPointcut()")
	public Object around(ProceedingJoinPoint pjp) throws Throwable {
		try {			
			DataSourceContextHolder.get().forceMaster();
			return pjp.proceed();
		} finally {
			DataSourceContextHolder.get().clear();
		}
	}
}
