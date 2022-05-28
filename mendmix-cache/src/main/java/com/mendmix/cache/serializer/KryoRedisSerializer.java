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
package com.mendmix.cache.serializer;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.mendmix.common.util.SerializeUtils;

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
