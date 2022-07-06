/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.mybatis.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.constants.MatchPolicy;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.model.OrderBy;
import com.mendmix.common.model.Page;
import com.mendmix.common.model.PageParams;
import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.mybatis.plugin.pagination.PageExecutor;
import com.mendmix.mybatis.plugin.pagination.PageExecutor.PageDataLoader;
import com.mendmix.mybatis.test.entity.UserEntity;
import com.mendmix.mybatis.test.mapper.SnsAccounyBindingEntityMapper;
import com.mendmix.mybatis.test.mapper.UserEntityMapper;
import com.mendmix.spring.InstanceFactory;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:test-mybatis.xml"})
@Rollback(false)
public class BaseMybatisTest implements ApplicationContextAware{
	
	@Autowired UserEntityMapper userMapper;
	@Autowired SnsAccounyBindingEntityMapper snsAccounyBindingMapper;

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {	
		InstanceFactory.setApplicationContext(arg0);
	}
	
	@Before
	public void init(){
		CurrentRuntimeContext.setTenantId("1");
		AuthUser user = new AuthUser();
		user.setId("user01");
		user.setDeptId("dept01");
		user.setName("admin");
		CurrentRuntimeContext.setAuthUser(user);
		MybatisRuntimeContext.addDataPermissionValues("type", "0");
		MybatisRuntimeContext.addDataPermissionValues("organization", MatchPolicy.exact.name());
		
	}
	
	@Test
	public void testUpdateVersion(){
		UserEntity entity = userMapper.selectByPrimaryKey(4);
		//userMapper.insert(entity);
		//userMapper.deleteByPrimaryKey(entity.getId());
		entity.setName(RandomStringUtils.random(5, true, false));
		entity.setStatus((short) 1);
		entity.setVersion(4);
		int result = userMapper.updateByPrimaryKeyWithVersion(entity);
		System.out.println(result);

	}
	
	@Test
	public void testInsertSelective(){
		UserEntity entity = buildUserEntity();
		userMapper.insertSelective(entity);
		userMapper.deleteByPrimaryKey(entity.getId());
	}
	
	@Test
	public void testInsertList() throws InterruptedException{
		List<UserEntity> entities = new ArrayList<>(2);
		entities.add(buildUserEntity());
		entities.add(buildUserEntity());
		userMapper.insertList(entities);
		Thread.sleep(10000);
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
	public void testSelectByPrimaryKey(){
		userMapper.selectByPrimaryKey(1);
		List<UserEntity> list = userMapper.selectByPrimaryKeys(Arrays.asList(4,5,6));
		System.out.println(list.size());
	}
	
	@Test
	public void testSelectByExample() throws InterruptedException{
		UserEntity example = new UserEntity();
		example.setName("嘎子");
		example.setType((short) 1);
		example.setCreatedAt(new Date());
		userMapper.selectByExample(example);
	}
	
	@Test
	public void testCountByExample() throws InterruptedException{
		UserEntity example = new UserEntity();
		example.setName("嘎子");
		example.setType((short) 1);
		example.setCreatedAt(new Date());
		userMapper.countByExample(example);
	}
	
	
	@Test
	public void testPage(){
		Page<UserEntity> pageInfo;
		UserEntity example = new UserEntity();
		example.setType((short)1);
		PageParams pageParams = new PageParams(1,10,new OrderBy("name"));
		pageInfo = PageExecutor.pagination(pageParams, new PageDataLoader<UserEntity>() {
			@Override
			public List<UserEntity> load() {
				return userMapper.selectByExample(example);
			}
		});
		
		System.out.println(pageInfo);
		
	}
	
	
	@Test
	public void testQuery(){
		Map<String, Object> param = new HashMap<>();
		param.put("status", 1);
		
		userMapper.findByStatus((short)1);
	}
	
	
	@Test
	public void testXXXX(){
		UserEntity entity = userMapper.selectByPrimaryKey(8);
		userMapper.findMobileByIds(Arrays.asList(4,5,6));
		
		userMapper.findByMobile(entity.getMobile());
		userMapper.findByType((short)1);
		
		entity = userMapper.selectByPrimaryKey(8);
		entity.setName(RandomStringUtils.random(5, true, true));
		userMapper.updateByPrimaryKeySelective(entity);
		
		userMapper.findMobileByIds(Arrays.asList(4,5,6));
		userMapper.findByType((short)1);
	}
	
	@Test
	public void testUpdate() throws InterruptedException{
		UserEntity example = new UserEntity();
		example.setStatus((short)1);
		example.setType((short)1);
		example.setName("嘎子");
		example.setMobile("13800373090");
		//userMapper.updateByExample(example);
		//
		//userMapper.updateType(1, new int[] {1,2,3});
		//
		//userMapper.updateByName("嘎子");
		//
		Map<String, Object> param = new HashMap<>();
		param.put("status", example.getStatus());
		param.put("type", example.getType());
		param.put("name", example.getName());
		param.put("mobile", example.getMobile());
		//userMapper.updateByMap(param);
		
		userMapper.updateTypeByExample(example.getType(), example);
		
		Thread.sleep(60000);
	}

	private UserEntity buildUserEntity() {
		String mobile = "13800"+RandomStringUtils.random(6, false, true);
		UserEntity entity = new UserEntity();
		entity.setEmail(mobile + "@163.com");
		entity.setMobile(mobile);
		entity.setType((short)1);
		entity.setStatus((short)1);
		return entity;
	}
	
}
