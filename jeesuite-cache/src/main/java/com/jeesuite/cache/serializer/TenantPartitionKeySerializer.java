package com.jeesuite.cache.serializer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.jeesuite.common.CurrentRuntimeContext;

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
