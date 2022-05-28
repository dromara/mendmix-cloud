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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.mendmix.common.CurrentRuntimeContext;

public class TenantPartitionKeySerializer implements RedisSerializer<String>{

	private final static String TENANT_KEY_TEMPLATE= "_tenant@%s:%s";
	
	private final Charset charset;


	public TenantPartitionKeySerializer() {
		this(StandardCharsets.UTF_8);
	}

	public TenantPartitionKeySerializer(Charset charset) {
		Assert.notNull(charset, "Charset must not be null!");
		this.charset = charset;
	}

	@Override
	public String deserialize(@Nullable byte[] bytes) {
		if(bytes == null)return null;
		String key = new String(bytes, charset);
		return key;
	}


	@Override
	public byte[] serialize(@Nullable String string) {
		if(string == null)return null;
		String tenantId = CurrentRuntimeContext.getTenantId();
		if(tenantId != null) {
			string  = String.format(TENANT_KEY_TEMPLATE, tenantId,string);
		}
		return string.getBytes(charset);
	}

	@Override
	public Class<?> getTargetType() {
		return String.class;
	}

}
