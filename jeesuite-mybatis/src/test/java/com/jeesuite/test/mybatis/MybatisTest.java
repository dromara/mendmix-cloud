/**
 * 
 */
package com.jeesuite.test.mybatis;

import java.util.Date;
import java.util.List;
import java.util.Set;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.jeesuite.cache.redis.JedisProviderFactory;
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
	
	@Autowired TransactionTemplate transactionTemplate;

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
		
		mapper.countByType(1);
	
		Set<String> keys = JedisProviderFactory.getMultiKeyCommands(null).keys("UserEntity*");
		System.out.println(keys);
		mapper.deleteByKey(1);
		mapper.deleteByKey(29);
		userEntity.setName("demo");
		mapper.updateByKeySelective(userEntity);
		
		mapper.countByType(2);
		
		mapper.updateType2(userEntity);
		
		mapper.findByStatus((short)1);
		mapper.findByStatus((short)2);
		
		mapper.updateType(new int[]{20,21}, 2);
		
		userEntity.setId(null);
		mapper.countByExample(userEntity);
		
		System.out.println();
	}
	
	
	@Test
	@Transactional
	public void testRwRouteWithTransaction(){
		mapper.findByStatus((short)2);
		
		UserEntity entity = new UserEntity();
		entity.setCreatedAt(new Date());
		entity.setEmail(RandomStringUtils.random(6, true, true) + "@163.com");
		entity.setMobile("13800"+RandomUtils.nextLong(100000, 999999));
		entity.setType((short)1);
		entity.setStatus((short)1);
		mapper.insert(entity);
	}
	
	@Test
	public void testRwRouteWithTransaction2(){
		
		mapper.findByStatus((short)1);
		
		transactionTemplate.execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {

				mapper.findByStatus((short)2);
				
				UserEntity entity = new UserEntity();
				entity.setCreatedAt(new Date());
				entity.setEmail(RandomStringUtils.random(6, true, true) + "@163.com");
				entity.setMobile("13800"+RandomUtils.nextLong(100000, 999999));
				entity.setType((short)1);
				entity.setStatus((short)1);
				mapper.insert(entity);
				
				mapper.findByStatus((short)2);
				
				return null;
			}
		});
		System.out.println();
	}
	

	public static void main(String[] args) {
		System.out.println(List.class.isAssignableFrom(Iterable.class) );
	}
	
}
