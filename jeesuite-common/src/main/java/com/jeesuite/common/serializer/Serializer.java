package com.jeesuite.common.serializer;

import java.io.IOException;

/**
 * 对象序列化接口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月28日
 */
public interface Serializer {
	
	public String name();

	public byte[] serialize(Object obj) throws IOException ;
	
	public Object deserialize(byte[] bytes) throws IOException ;
	
}
