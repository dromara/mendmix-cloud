package com.jeesuite.kafka.serializer;

import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;

public class ZKStringSerializer implements ZkSerializer{

	private String encoding = "UTF8";
	@Override
	public byte[] serialize(Object data) throws ZkMarshallingError {
		return data.toString().getBytes();
	}

	@Override
	public Object deserialize(byte[] bytes) throws ZkMarshallingError {
		return new String(bytes);
	}

}
