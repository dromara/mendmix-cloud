/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.example.dao.entity.ExampleStaffEntity;
import org.dromara.mendmix.example.dao.mapper.ExampleCompanyEntityMapper;
import org.dromara.mendmix.example.dao.mapper.ExampleStaffEntityMapper;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.spring.InstanceFactory;
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
	
	@Test
	public void testFindByExample() {
		ExampleStaffEntity example = new ExampleStaffEntity();
		example.setName("jim");
		List<ExampleStaffEntity> list = staffMapper.selectByExample(example);
		System.out.println(list.size());
	}
	
}
