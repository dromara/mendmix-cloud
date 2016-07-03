package com.jeesuite.kafka.serializer;

import org.apache.commons.lang3.SerializationUtils;

import kafka.serializer.Decoder;


/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月14日
 */
public class MessageDecoder implements Decoder<Object> {
	
    @Override
    public Object fromBytes(byte[] bytes) {
    	return SerializationUtils.deserialize(bytes);
    }
}
