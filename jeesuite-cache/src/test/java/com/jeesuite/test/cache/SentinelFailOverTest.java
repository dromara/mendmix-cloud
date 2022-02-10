/**
 * 
 */
package com.jeesuite.test.cache;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.jeesuite.cache.command.RedisString;
import com.jeesuite.spring.InstanceFactory;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:test-cache.xml"})
public class SentinelFailOverTest implements ApplicationContextAware{
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {	
		InstanceFactory.setApplicationContext(arg0);
	}
	
	@Test
	public void testRedisString(){
		int count = 0;
		while(true){	
			if(count == 1000)break;
			RedisString redis = new RedisString("foo"+RandomUtils.nextInt(100, 999999));
			redis.set("bar",10);
			System.out.println(String.format("val:%s,ttl:%s", redis.get(),redis.getTtl()));
			count++;
			try {Thread.sleep(300);} catch (Exception e) {}
		}
	}
	
}
