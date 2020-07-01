/**
 * 
 */
package com.jeesuite.mybatis.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.jeesuite.mybatis.MybatisRuntimeContext;
import com.jeesuite.mybatis.test.entity.UserEntity;
import com.jeesuite.mybatis.test.mapper.UserEntityMapper;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:test-mybatis.xml"})
@Rollback(false)
public class BaseMybatisTest implements ApplicationContextAware{
	
	@Autowired UserEntityMapper userMapper;

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {	
		InstanceFactory.setInstanceProvider(new SpringInstanceProvider(arg0));
	}
	
	@Before
	public void init(){
		MybatisRuntimeContext.setTenantId("1000");
	}
	
	@Test
	public void testInsert(){
		UserEntity entity = buildUserEntity();
		userMapper.insert(entity);
		userMapper.deleteByPrimaryKey(entity.getId());
	}
	
	@Test
	public void testInsertSelective(){
		UserEntity entity = buildUserEntity();
		userMapper.insertSelective(entity);
		userMapper.deleteByPrimaryKey(entity.getId());
	}
	
	@Test
	public void testInsertList(){
		List<UserEntity> entities = new ArrayList<>(2);
		entities.add(buildUserEntity());
		entities.add(buildUserEntity());
		userMapper.insertList(entities);
	}
	
	@Test
	public void testUpdateByPrimaryKey(){
		UserEntity entity = userMapper.selectByPrimaryKey(4);
		entity.setName("jim");
		userMapper.updateByPrimaryKey(entity);
		System.out.println("name:" + entity.getName());
	}
	
	@Test
	public void testUpdateByPrimaryKeySelective(){
		UserEntity entity = userMapper.selectByPrimaryKey(4);
		entity.setName("jim2");
		userMapper.updateByPrimaryKeySelective(entity);
		System.out.println("name:" + entity.getName());
	}
	
	@Test
	public void testSelectAll(){
		List<UserEntity> list = userMapper.selectAll();
		long count = userMapper.countAll();
		System.out.println(list.size() + " - " + count);
	}
	
	@Test
	public void testSelectByPrimaryKeys(){
		List<UserEntity> list = userMapper.selectByPrimaryKeys(Arrays.asList(4,5,6));
		System.out.println(list.size());
	}
	
	@Test
	public void testSelectByExample(){
		UserEntity example = new UserEntity();
		example.setStatus((short)1);
		example.setType((short)1);
		List<UserEntity> list = userMapper.selectByExample(example);
		
		long count = userMapper.countByExample(example);
		System.out.println(list.size() + " - " + count);
	}

	private UserEntity buildUserEntity() {
		String mobile = "13800"+RandomStringUtils.random(6, false, true);
		UserEntity entity = new UserEntity();
		entity.setCreatedAt(new Date());
		entity.setEmail(mobile + "@163.com");
		entity.setMobile(mobile);
		entity.setType((short)1);
		entity.setStatus((short)1);
		return entity;
	}
	
}
