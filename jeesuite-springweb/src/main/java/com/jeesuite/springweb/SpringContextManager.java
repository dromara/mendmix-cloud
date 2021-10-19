package com.jeesuite.springweb;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.jeesuite.common.async.RetryAsyncTaskExecutor;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;
import com.jeesuite.springweb.logging.RequestLogCollector;

/**
 * 
 * <br>
 * Class Name   : SpringContextManager
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年9月13日
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpringContextManager implements ApplicationContextAware,DisposableBean{

	@Override
	public void destroy() throws Exception {
		RetryAsyncTaskExecutor.close();
		RequestLogCollector.destroy();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		InstanceFactory.setInstanceProvider(new SpringInstanceProvider(applicationContext));
	}

}
