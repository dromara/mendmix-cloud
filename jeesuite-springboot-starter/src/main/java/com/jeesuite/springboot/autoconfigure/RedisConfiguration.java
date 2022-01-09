package com.jeesuite.springboot.autoconfigure;

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

import com.jeesuite.cache.adapter.RedisCacheAdapter;
import com.jeesuite.cache.serializer.KryoRedisSerializer;
import com.jeesuite.cache.serializer.TenantPartitionKeySerializer;

@Configuration
@ConditionalOnProperty(name = {"spring.redis.database"})
public class RedisConfiguration {

	@Value("${jeesuite.redis.keyUseStringSerializer:true}")
	private boolean keyUseStringSerializer;
	@Value("${jeesuite.redis.valueSerializerType:}")
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
