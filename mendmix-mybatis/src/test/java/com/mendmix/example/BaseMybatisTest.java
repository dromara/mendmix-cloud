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
package com.mendmix.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
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
import com.mendmix.common.model.AuthUser;
import com.mendmix.example.dao.entity.ExampleStaffEntity;
import com.mendmix.example.dao.mapper.ExampleCompanyEntityMapper;
import com.mendmix.example.dao.mapper.ExampleStaffEntityMapper;
import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.spring.InstanceFactory;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:test-mybatis.xml"})
@Rollback(false)
public class BaseMybatisTest implements ApplicationContextAware{
	
	@Autowired ExampleStaffEntityMapper staffMapper;
	@Autowired ExampleCompanyEntityMapper companyMapper;

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
		//
		MybatisRuntimeContext.addDataPermissionValues("area", "HN","HB");
		MybatisRuntimeContext.addDataPermissionValues("companyId", "1","2","3");
	}
	
	@Test
	public void testBase() {
		staffMapper.selectByPrimaryKey(5);
		ExampleStaffEntity staffEntity = staffMapper.selectByPrimaryKey(5);
		staffEntity.setName(RandomStringUtils.random(5, true, false));
		staffMapper.updateByPrimaryKeySelective(staffEntity);
	}
	
	@Test
	public void testQueryStaffList() {
		Map<String, Object> param = new HashMap<>();
		param.put("status", 1);
		List<ExampleStaffEntity> list = staffMapper.findListByParam(param);
		System.out.println(list.size());
	}
	
}
