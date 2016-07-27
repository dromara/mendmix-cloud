/**
 * 
 */
package com.jeesuite.mybatis.spring;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.ApplicationContext;

import com.jeesuite.mybatis.crud.GeneralSqlGenerator;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月17日
 * @Copyright (c) 2015, jwww
 */
public class MapperScannerConfigurer extends org.mybatis.spring.mapper.MapperScannerConfigurer{

	private ApplicationContext context;
	private String sqlSessionFactoryName;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		super.setApplicationContext(applicationContext);
		this.context = applicationContext;
	}
	
	@Override
	public void setSqlSessionFactoryBeanName(String sqlSessionFactoryName) {
		super.setSqlSessionFactoryBeanName(sqlSessionFactoryName);
		this.sqlSessionFactoryName = sqlSessionFactoryName;
	}



	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		SqlSessionFactory sqlSessionFactory = context.getBean(sqlSessionFactoryName, SqlSessionFactory.class);
		Configuration configuration = sqlSessionFactory.getConfiguration();
		//
		GeneralSqlGenerator.generate(configuration);
	}	
}
