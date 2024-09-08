/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.common.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.dromara.mendmix.common.ThreadLocalContext;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月28日
 */
public class KryoSerializer implements Serializer {

    private static final String KRYO_INSTANCE_ID = "kryo";

	public static Kryo getKryo() {
    	Kryo kryo = ThreadLocalContext.get(KRYO_INSTANCE_ID);
    	if(kryo == null) {
    		kryo = new Kryo();
    		kryo.setRegistrationRequired(false);
    		kryo.setWarnUnregisteredClasses(false);
    		ThreadLocalContext.set(KRYO_INSTANCE_ID, kryo);
    	}
		return kryo;
	}

	@Override
	public String name() {
		return KRYO_INSTANCE_ID;
	}

	@Override
	public byte[] serialize(Object obj) throws IOException {
		Output output = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			output = new Output(baos);
			getKryo().writeClassAndObject(output, obj);
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
			return getKryo().readClassAndObject(ois);
		} finally {
			if(ois != null)
				ois.close();
		}
	}
	
}
