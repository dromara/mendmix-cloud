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
package org.dromara.mendmix.common.util;

import java.io.IOException;

import org.dromara.mendmix.common.serializer.KryoPoolSerializer;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月28日
 */
public class SerializeUtils {

	static KryoPoolSerializer serializer = new KryoPoolSerializer();
    /**
     * 序列化
     *
     * @param object 需要序列化的对象
     * @return
     */
    public static byte[] serialize(Object object) {
    	try {
			return serializer.serialize(object);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    /**
     * 反序列化
     *
     * @param bytes 需要被反序列化的数据
     * @return
     */
    public static Object deserialize(byte[] bytes) {
    	try {
			return serializer.deserialize(bytes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }
}
