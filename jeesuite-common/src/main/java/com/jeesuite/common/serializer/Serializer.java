package com.jeesuite.common.serializer;

import java.io.IOException;

/**
 * 对象序列化接口
 * @description <br>
 * @author <a href="mailto:wei.jiang@lifesense.com">vakin</a>
 * @date 2015年11月24日
 * @Copyright (c) 2015, lifesense.com
 */
public interface Serializer {
	
	public String name();

	public byte[] serialize(Object obj) throws IOException ;
	
	public Object deserialize(byte[] bytes) throws IOException ;
	
}
