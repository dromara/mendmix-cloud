/**
 * 
 */
package com.jeesuite.common.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月28日
 */
public class KryoSerializer implements Serializer {

	private final static Kryo kryo;
	static {
		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setWarnUnregisteredClasses(false);
	}


    @Override
	public String name() {
		return "kryo";
	}

	@Override
	public byte[] serialize(Object obj) throws IOException {
		Output output = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			output = new Output(baos);
			kryo.writeClassAndObject(output, obj);
			output.flush();
			return baos.toByteArray();
		}finally{
			if(output != null)
				output.close();
		}
	}

	@Override
	public Object deserialize(byte[] bits) throws IOException {
		if(bits == null || bits.length == 0)
			return null;
		Input ois = null;
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bits);
			ois = new Input(bais);
			return kryo.readClassAndObject(ois);
		} finally {
			if(ois != null)
				ois.close();
		}
	}
	
}
