package com.jeesuite.common.json;

import java.util.List;

public class JsonUtils {
	
	
	private static JsonMapper jsonMapper = JsonMapper.getDefault();
	
	public static String toJson(Object object) {
		return jsonMapper.toJson(object);
	}
	/**
	 * 不含值为null的属性
	 * @param object
	 * @return
	 */
	public static String toJsonIgnoreNullField(Object object) {
		return JsonMapper.nonNullMapper().toJson(object);
	}
	
	public static <T> T toObject(String jsonString, Class<T> clazz) {
		return  jsonMapper.toObject(jsonString, clazz);
	}
	
	public static <T> List<T> toList(String jsonString, Class<T> clazz) {
		return jsonMapper.toList(jsonString, clazz);
	}
}