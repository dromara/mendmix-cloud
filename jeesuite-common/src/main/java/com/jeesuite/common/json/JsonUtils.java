package com.jeesuite.common.json;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
	
	
	private static JsonMapper jsonMapper = JsonMapper.getDefault();
	
	public static ObjectMapper getMapper(){
		return jsonMapper.getMapper();
	}
	
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
	
	/**
	 * 转换成格式化的json字符串
	 * @param object
	 * @return
	 */
	public static String toPrettyJson(Object object){
		try {			
			return getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T> T toObject(String jsonString, Class<T> clazz) {
		return  jsonMapper.toObject(jsonString, clazz);
	}
	
	public static <T> List<T> toList(String jsonString, Class<T> clazz) {
		return jsonMapper.toList(jsonString, clazz);
	}
	
	public static <K,V> Map<K, V> toHashMap(String jsonString, Class<K> keyType, Class<V> valueType) {
		return jsonMapper.toHashMap(jsonString, keyType, valueType);
	}
	
	public static <V> Map<String, V> toHashMap(String jsonString, Class<V> valueType) {
		return jsonMapper.toHashMap(jsonString, String.class, valueType);
	}
	
	public static JsonNode getNode(String jsonString,String nodeName){
		try {
			JsonNode node = jsonMapper.getMapper().readTree(jsonString);	
			return nodeName == null ? node : node.get(nodeName);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 
	 * @param jsonString
	 * @param attrs (e.g:info.user.id)
	 * @return
	 */
	public static String getJsonNodeValue(String jsonString, String attrs) {  
		return getJsonNodeValue(getNode(jsonString, null), attrs);
	}
	
	/**
	 * 
	 * @param node
	 * @param attrs (e.g:info.user.id)
	 * @return
	 */
	public static String getJsonNodeValue(JsonNode node, String attrs) {  
        int index = attrs.indexOf('.');  
        if (index == -1) {  
            if (node != null && node.get(attrs) != null) {  
                return node.get(attrs).asText();
            }  
            return null;  
        } else {  
            String s1 = attrs.substring(0, index);  
            String s2 = attrs.substring(index + 1);  
            return getJsonNodeValue(node.get(s1), s2);  
        }  
    }  
	
	
}