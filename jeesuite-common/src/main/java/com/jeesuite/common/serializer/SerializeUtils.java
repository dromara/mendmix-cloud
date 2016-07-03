package com.jeesuite.common.serializer;

import java.io.IOException;

/**
 * 对象序列化工具
 * @description <br>
 * @author <a href="mailto:wei.jiang@lifesense.com">vakin</a>
 * @date 2015年10月30日
 * @Copyright (c) 2015, lifesense.com
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
