package com.jeesuite.springweb;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.jeesuite.common.concurrent.RetryAsyncTaskExecutor;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;

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
public class SpringContextManager implements ApplicationContextAware,DisposableBean{

	@Override
	public void destroy() throws Exception {
		RetryAsyncTaskExecutor.close();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		InstanceFactory.setInstanceProvider(new SpringInstanceProvider(applicationContext));
	}

}
