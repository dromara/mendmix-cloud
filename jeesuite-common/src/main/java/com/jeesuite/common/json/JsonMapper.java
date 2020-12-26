package com.jeesuite.common.json;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.JSONPObject;


public class JsonMapper {

	private static JsonMapper defaultMapper;

	private ObjectMapper mapper;

	public JsonMapper() {
		this(null);
	}

	public JsonMapper(Include include) {
		mapper = new ObjectMapper();
		//设置输出时包含属性的风格
		if (include != null) {
			mapper.setSerializationInclusion(include);
		}
		//设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		//
		mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, false);
	}
	
	public JsonMapper enumAndStringConvert(boolean enabled) {
		if(enabled){
			mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
			mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		}else{
			mapper.disable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
			mapper.disable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		}
		return this;
		
	}
	
	public JsonMapper dateAndTimestampConvert(boolean enabled) {
		if(enabled){
			mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
			mapper.enable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
		}else{
			mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
			mapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
			mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
		}
		return this;
		
	}

	public static JsonMapper nonEmptyMapper() {
		return new JsonMapper(Include.NON_EMPTY);
	}
	
	public static JsonMapper nonNullMapper() {
		return new JsonMapper(Include.NON_NULL);
	}

	public static JsonMapper nonDefaultMapper() {
		return new JsonMapper(Include.NON_DEFAULT);
	}


	public String toJson(Object object) {

		try {
			return mapper.writeValueAsString(object);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public <T> T toObject(String jsonString, Class<T> clazz) {
		if (StringUtils.isEmpty(jsonString)) {
			return null;
		}

		try {
			return mapper.readValue(jsonString, clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public <T> List<T> toList(String jsonString, Class<T> elementType) {
		if (StringUtils.isEmpty(jsonString)) {
			return null;
		}

		JavaType javaType = mapper.getTypeFactory().constructParametricType(ArrayList.class, elementType);
		return toObject(jsonString, javaType);
	}
	
	public <K,V> Map<K, V> toHashMap(String jsonString, Class<K> keyType, Class<V> valueType) {
		if (StringUtils.isEmpty(jsonString)) {
			return null;
		}
		JavaType javaType = mapper.getTypeFactory().constructParametricType(HashMap.class, keyType,valueType);
		return toObject(jsonString, javaType);
	}


	@SuppressWarnings("unchecked")
	public <T> T toObject(String jsonString, JavaType javaType) {
		if (StringUtils.isEmpty(jsonString)) {
			return null;
		}

		try {
			return (T) mapper.readValue(jsonString, javaType);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T update(String jsonString, T object) {
		try {
			return (T) mapper.readerForUpdating(object).readValue(jsonString);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String toJsonP(String functionName, Object object) {
		return toJson(new JSONPObject(functionName, object));
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	public static JsonMapper getDefault() {
		if(defaultMapper == null){
			defaultMapper = new JsonMapper();
			defaultMapper.enumAndStringConvert(true);
			defaultMapper.dateAndTimestampConvert(true);
		}
		return defaultMapper;
	}
	
	
}
