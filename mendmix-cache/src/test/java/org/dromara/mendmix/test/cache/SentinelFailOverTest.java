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
package org.dromara.mendmix.test.cache;

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

import org.dromara.mendmix.cache.command.RedisString;
import org.dromara.mendmix.spring.InstanceFactory;


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
