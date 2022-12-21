/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.springcloud.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mendmix.cache.CacheUtils;
import com.mendmix.common.WorkIdGenerator;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.common2.workerid.LocalWorkIdGenerator;
import com.mendmix.common2.workerid.RedisWorkIdGenerator;
import com.mendmix.common2.workerid.ZkWorkIdGenerator;

@Configuration
public class IdWorkIdConfiguration {
	
    @Bean
    public WorkIdGenerator workIdGenerator() {
    	if(ResourceUtils.containsProperty("mendmix.zookeeper.servers")) {
    		try {
				Class.forName("org.apache.zookeeper.ZooKeeper");
				return new ZkWorkIdGenerator();
			} catch (ClassNotFoundException e) {}
    	}
    	if(CacheUtils.isRedis()) {
    		try {
				Class.forName("org.springframework.data.redis.core.StringRedisTemplate");
				return new RedisWorkIdGenerator();
			} catch (ClassNotFoundException e) {}
    	}
    	return new LocalWorkIdGenerator();
    }
}
