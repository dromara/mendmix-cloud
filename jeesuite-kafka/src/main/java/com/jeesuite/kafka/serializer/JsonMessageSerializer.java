package com.jeesuite.kafka.serializer;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import com.jeesuite.common.json.JsonUtils;


/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年3月11日
 */
public class JsonMessageSerializer implements Serializer<Serializable> {

    /**
     * Configure this class.
     *
     * @param configs configs in key/value pairs
     * @param isKey   whether is for key or value
     */
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    /**
     * serialize
     *
     * @param topic topic associated with data
     * @param data  typed data
     * @return serialized bytes
     */
    @Override
    public byte[] serialize(String topic, Serializable data) {
        try {
            if (data == null)
                return null;
            else{     
            	String toString = isSimpleDataType(data) ? data.toString() : JsonUtils.toJson(data);
            	return  toString.getBytes(StandardCharsets.UTF_8.name());
            }
        } catch (UnsupportedEncodingException e) {
            throw new SerializationException("Error when serializing string to byte[] due to unsupported encoding UTF-8");
        }
    
    }

    /**
     * Close this serializer
     */
    @Override
    public void close() {

    }
    
    private static boolean isSimpleDataType(Object o) {   
 	   Class<? extends Object> clazz = o.getClass();
        return 
        (   
            clazz.equals(String.class) ||   
            clazz.equals(Integer.class)||   
            clazz.equals(Byte.class) ||   
            clazz.equals(Long.class) ||   
            clazz.equals(Double.class) ||   
            clazz.equals(Float.class) ||   
            clazz.equals(Character.class) ||   
            clazz.equals(Short.class) ||   
            clazz.equals(BigDecimal.class) ||     
            clazz.equals(Boolean.class) ||   
            clazz.isPrimitive()   
        );   
    }
}
