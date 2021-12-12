package com.jeesuite.cache.serializer;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.jeesuite.common.util.SerializeUtils;

public class KryoRedisSerializer implements RedisSerializer<Object>{

	@Override
	public byte[] serialize(Object t) throws SerializationException {
		return SerializeUtils.serialize(t);
	}

	@Override
	public Object deserialize(byte[] bytes) throws SerializationException {
		return SerializeUtils.deserialize(bytes);
	}

}
