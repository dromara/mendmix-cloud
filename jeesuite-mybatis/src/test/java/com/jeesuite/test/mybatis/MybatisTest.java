/**
 * 
 */
package com.jeesuite.test.mybatis;

import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.jeesuite.mybatis.test.entity.UserEntity;
import com.jeesuite.mybatis.test.mapper.UserEntityMapper;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:test-mybatis.xml"})
@Rollback(false)
public class MybatisTest implements ApplicationContextAware{
	
	@Autowired UserEntityMapper mapper;

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {	
		InstanceFactory.setInstanceProvider(new SpringInstanceProvider(arg0));
	}
	
	@Test
	public void test(){
		UserEntity entity = new UserEntity();
		entity.setCreatedAt(new Date());
		entity.setEmail(RandomStringUtils.random(6, true, true) + "@163.com");
		entity.setMobile("13800"+RandomUtils.nextLong(100000, 999999));
		mapper.insert(entity);
		
		entity = new UserEntity();
		entity.setCreatedAt(new Date());
		entity.setEmail(RandomStringUtils.random(6, true, true) + "@163.com");
		entity.setMobile("13800"+RandomUtils.nextLong(100000, 999999));
		mapper.insertSelective(entity);
		
		mapper.getByKey(entity.getId());
		
		System.out.println(entity.getId());
		
		entity.setName("test..");
		mapper.update(entity);
		
		entity.setMobile(null);
		entity.setEmail(null);
		entity.setName("test..22");
		mapper.updateSelective(entity);
	}
	
}
