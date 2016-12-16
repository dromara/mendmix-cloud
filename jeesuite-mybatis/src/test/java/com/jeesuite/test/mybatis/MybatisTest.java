/**
 * 
 */
package com.jeesuite.test.mybatis;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

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

import com.jeesuite.mybatis.plugin.cache.EntityCacheHelper;
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
	public void testCRUD(){
		
		for (int i = 0; i < 5; i++) {
			
			UserEntity entity = new UserEntity();
			entity.setCreatedAt(new Date());
			entity.setEmail(RandomStringUtils.random(6, true, true) + "@163.com");
			entity.setMobile("13800"+RandomUtils.nextLong(100000, 999999));
			entity.setType((short)1);
			entity.setStatus((short)1);
			mapper.insert(entity);
			
			entity = new UserEntity();
			entity.setCreatedAt(new Date());
			entity.setEmail(RandomStringUtils.random(6, true, true) + "@163.com");
			entity.setMobile("13800"+RandomUtils.nextLong(100000, 999999));
			entity.setType((short)2);
			entity.setStatus((short)2);
			mapper.insertSelective(entity);
		}
		
		
	}
	
	@Test
	public void testCache(){
		System.out.println("------------");
		UserEntity userEntity = mapper.getByKey(20);
		mapper.findByMobile("13800951371");
		System.out.println("------------");
		mapper.findByMobile("13800639997");
		System.out.println("------------");
		mapper.findByMobile("13800639997");
		
		mapper.findByStatus((short)1);
		mapper.findByStatus((short)2);
		mapper.findByStatus((short)1);
		mapper.findByStatus((short)2);
//		
		mapper.findByType((short)1);
		mapper.findByType((short)1);
		
		//生成的缓存key为：UserEntity.findByStatus:2
		EntityCacheHelper.queryTryCache(UserEntity.class, "findByStatus:2", new Callable<List<UserEntity>>() {
			public List<UserEntity> call() throws Exception {
				//查询语句
				List<UserEntity> entitys = mapper.findByStatus((short)2);
				return entitys;
			}
		});
	
		mapper.deleteByKey(1);
		mapper.deleteByKey(29);
		userEntity.setName("demo");
		mapper.updateByKeySelective(userEntity);
	}
	
	
	@Test
	public void testCache2(){
		
	}
	

	public static void main(String[] args) {
		System.out.println(List.class.isAssignableFrom(Iterable.class) );
	}
	
}
