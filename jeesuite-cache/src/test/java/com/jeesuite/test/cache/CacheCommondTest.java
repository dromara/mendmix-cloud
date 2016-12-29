/**
 * 
 */
package com.jeesuite.test.cache;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.jeesuite.cache.command.RedisHashMap;
import com.jeesuite.cache.command.RedisNumber;
import com.jeesuite.cache.command.RedisObject;
import com.jeesuite.cache.command.RedisSortSet;
import com.jeesuite.cache.command.RedisString;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:test-cache.xml"})
public class CacheCommondTest implements ApplicationContextAware{
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {	
		InstanceFactory.setInstanceProvider(new SpringInstanceProvider(arg0));
	}
	
	@Test
	public void testRedisString(){
		//字符串
		RedisString redisString = new RedisString("User.id:1001");
		redisString.set("user1001",60);
		String value = redisString.get();
		
		redisString.getTtl();
		redisString.exists();
		redisString.setExpire(300);
		redisString.remove();
		
		//对象
		RedisObject redisObject = new RedisObject("User.id:1001");
		redisObject.set(new User(1001, "jack"));
		Object user = redisObject.get();
		redisObject.getTtl();
		redisObject.exists();
		redisObject.setExpire(300);
		redisObject.remove();
		
		//hash 
		RedisHashMap redisHashMap = new RedisHashMap("User.all");
		redisHashMap.set("1001", new User(1001, "jack"));
		redisHashMap.set("1002", new User(1002, "jack2"));
		
		Map<String, User> users = redisHashMap.get("1001","1002");
		users = redisHashMap.getAll();
		User one = redisHashMap.getOne("1001");
		
		redisHashMap.containsKey("1001");
		
		redisHashMap.remove();
		
		//
		RedisSortSet sortSet = new RedisSortSet("redisSortSet");
		
		sortSet.add(1, "1");
		sortSet.add(2, "2");
		sortSet.add(3, "3");
		sortSet.add(4, "4");
		
		System.out.println("==============");
		System.out.println(sortSet.get());
		
		sortSet.removeByScore(1, 2);
		
		System.out.println(sortSet.get());
		
	}
	
	@Test
	public void testRedisNumber(){
		long increase = new RedisNumber("ins_test").increase(5);
		System.out.println(increase);
		System.out.println(new RedisNumber("ins_test").increase(5));
	}
	
	@Test
	public void testLocalRedis() throws InterruptedException{
		//字符串
		RedisString redisString = new RedisString("User.id:1001");
		redisString.set("user1001",60);
		String value = redisString.get();
		System.out.println(value);
		value = redisString.get();
		System.out.println(value);
		
		redisString.remove();
		
		Thread.sleep(5000);
	}
	
	@Test
	public void test11(){
		
	}
	
}
