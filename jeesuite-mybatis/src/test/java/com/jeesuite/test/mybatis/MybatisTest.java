/**
 * 
 */
package com.jeesuite.test.mybatis;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.cache.EntityCacheHelper;
import com.jeesuite.mybatis.plugin.pagination.Page;
import com.jeesuite.mybatis.plugin.pagination.PageExecutor;
import com.jeesuite.mybatis.plugin.pagination.PageExecutor.PageDataLoader;
import com.jeesuite.mybatis.plugin.pagination.PageParams;
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
	
	static String[] mobiles = new String[10];

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {	
		InstanceFactory.setInstanceProvider(new SpringInstanceProvider(arg0));
	}
	
	@Test
	public void testCRUD(){
		
		for (int i = 0; i < mobiles.length; i++) {
			mobiles[i] = "13800"+RandomUtils.nextLong(100000, 999999);
			UserEntity entity = new UserEntity();
			entity.setCreatedAt(new Date());
			entity.setEmail(mobiles[i] + "@163.com");
			entity.setMobile(mobiles[i]);
			entity.setType((short)(i % 2 == 0 ? 1 : 2));
			entity.setStatus((short)(i % 3 == 0 ? 1 : 2));
			mapper.insert(entity);
		}
		
		
	}
	
	@Test
	public void testCache(){
		System.out.println("------------");
		UserEntity userEntity = mapper.getByKey(1);
		mapper.findByMobile(mobiles[0]);
		System.out.println("------------");
		mapper.findByMobile(mobiles[1]);
		System.out.println("------------");
		mapper.findByMobile(mobiles[2]);
		
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
		
		EntityCacheHelper.removeCache(UserEntity.class);
		System.out.println();
	}
	
	@Test
	public void testFindNotExistsThenInsert(){
		String mobile = "13800000002";
		UserEntity entity = mapper.findByMobile(mobile);
		if(entity != null){
			System.out.println(entity.getMobile());
			return;
		}
		entity = new UserEntity();
		entity.setCreatedAt(new Date());
		entity.setEmail(mobile + "@163.com");
		entity.setMobile(mobile);
		entity.setType((short)1);
		entity.setStatus((short)1);
		mapper.insert(entity);
	}
	
	@Test
	public void testFindBystatus(){
		List<UserEntity> list = mapper.findByStatus((short)1);
		for (UserEntity userEntity : list) {
			System.out.println(userEntity.getMobile());
		}
	}
	
	@Test
	public void testFindNotExists(){
		EntityInfo entityInfo = MybatisMapperParser.getEntityInfoByMapper("com.jeesuite.mybatis.test.mapper.UserEntityMapper");
		
		String sql = entityInfo.getMapperSqls().get("com.jeesuite.mybatis.test.mapper.UserEntityMapper.queryByExample");
		System.out.println(sql);
		
		System.out.println(sql.replaceAll("<(.*)>.*order by.*</(.*)>", ""));
	}
	
	@Test
	public void testPage(){
		Page<UserEntity> pageInfo = PageExecutor.pagination(new PageParams(1,10), new PageDataLoader<UserEntity>() {
			@Override
			public List<UserEntity> load() {
				UserEntity example = new UserEntity();
				example.setType((short)1);
				return mapper.queryByExample(example);
			}
		});
		
		System.out.println(pageInfo);
	}
	
	@Test
	public void testPage2(){
		Page<UserEntity> pageInfo = mapper.pageQuery(new PageParams(1,5));
		
		System.out.println(pageInfo);
	}
	
	@Test
	public void testFindMobileByIds(){
		List<String> mobiles = mapper.findMobileByIds(new ArrayList<>(Arrays.asList(21,23)));
		for (String mobile : mobiles) {
			System.out.println("------>>>>" + mobile);
		}
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
	
}
