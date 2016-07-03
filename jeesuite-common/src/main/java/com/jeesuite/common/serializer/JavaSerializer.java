/**
 * 
 */
package com.jeesuite.common.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * JDK标准实现序列化
 * @description <br>
 * @author <a href="mailto:wei.jiang@lifesense.com">vakin</a>
 * @date 2015年11月24日
 * @Copyright (c) 2015, lifesense.com
 */
public class JavaSerializer implements Serializer {

	@Override
	public String name() {
		return "java";
	}
	
	@Override
	public byte[] serialize(Object obj) throws IOException {
		ObjectOutputStream oos = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			return baos.toByteArray();
		} finally {
			if(oos != null)
			try {
				oos.close();
			} catch (IOException e) {}
		}
	}

	@Override
	public Object deserialize(byte[] bits) throws IOException {
		if(bits == null || bits.length == 0)
			return null;
		ObjectInputStream ois = null;
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bits);
			ois = new ObjectInputStream(bais);
			return ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			if(ois != null)
			try {
				ois.close();
			} catch (IOException e) {}
		}
	}
	
}
