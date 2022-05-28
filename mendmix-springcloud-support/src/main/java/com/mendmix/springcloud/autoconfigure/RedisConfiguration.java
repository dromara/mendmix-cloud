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
package com.mendmix.springcloud.autoconfigure;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.mendmix.cache.adapter.RedisCacheAdapter;
import com.mendmix.cache.serializer.KryoRedisSerializer;
import com.mendmix.cache.serializer.TenantPartitionKeySerializer;

@Configuration
@ConditionalOnProperty(name = {"spring.redis.database"})
public class RedisConfiguration {

	@Value("${mendmix.redis.keyUseStringSerializer:true}")
	private boolean keyUseStringSerializer;
	@Value("${mendmix.redis.valueSerializerType:}")
	private String valueSerializerType;
	
	@Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        //key
        if(keyUseStringSerializer) {
        	template.setKeySerializer(new TenantPartitionKeySerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
        }
      //value
        if("Kryo".equalsIgnoreCase(valueSerializerType)) {
        	KryoRedisSerializer kryoRedisSerializer = new KryoRedisSerializer();
            template.setValueSerializer(kryoRedisSerializer);
            template.setHashValueSerializer(kryoRedisSerializer);
        }else if("json".equalsIgnoreCase(valueSerializerType)) {
        	//FastJsonRedisSerializer
        	Jackson2JsonRedisSerializer<Object> jsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        	template.setValueSerializer(jsonRedisSerializer);
            template.setHashValueSerializer(jsonRedisSerializer);
        }

        template.afterPropertiesSet();
        return template;
    }
	
	@Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) throws UnknownHostException {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setKeySerializer(new TenantPartitionKeySerializer());
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
	
	@Bean
    public RedisCacheAdapter redisCacheAdapter(RedisTemplate<String, Object> redisTemplate,StringRedisTemplate stringRedisTemplate) {
		return new RedisCacheAdapter(redisTemplate, stringRedisTemplate);
	}
}
