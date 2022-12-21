/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.common.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.mendmix.common.GlobalConstants;

public class JsonUtils {
	
	
	private static ObjectMapper jsonMapper = null;
	
	private static String JSON_OBJECT_PREFIX = "{";
	
	private static String JSON_ARRAY_PREFIX = "[";
	
	
	public static void setObjectMapper(ObjectMapper jsonMapper) {
		JsonUtils.jsonMapper = jsonMapper;
	}

	public static ObjectMapper getMapper(){
		if(jsonMapper != null)return jsonMapper;
		synchronized (JsonUtils.class) {
			if(jsonMapper != null)return jsonMapper;
			if(jsonMapper == null) {
				jsonMapper = new ObjectMapper();
				//设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
				jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
				jsonMapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, false);
				jsonMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
				jsonMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
				jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
				jsonMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
				if(ResourceUtils.getBoolean("mendmix.jackson.ignoreNull",true)) {
					jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		        }else {
		        	jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		        }
				jsonMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
			}
		}
		return jsonMapper;
	}
	
	public static boolean isJsonString(String str) {
		return isJsonObjectString(str) || isJsonArrayString(str);
	}
	
    public static boolean isJsonObjectString(String str) {
		return StringUtils.trimToEmpty(str).startsWith(JSON_OBJECT_PREFIX);
	}
    
    public static boolean isJsonArrayString(String str) {
    	return StringUtils.trimToEmpty(str).startsWith(JSON_ARRAY_PREFIX);
	}
	
	public static String toJson(Object object) {
		try {
			return getMapper().writeValueAsString(object);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] toJsonBytes(Object object) {
		return toJson(object).getBytes(StandardCharsets.UTF_8);
	}
	
	/**
	 * 不含值为null的属性
	 * @param object
	 * @return
	 */
	public static String toJsonIgnoreNullField(Object object) {
		return toJson(object);
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
		if (StringUtils.isEmpty(jsonString)) {
			return null;
		}
		try {
			return getMapper().readValue(jsonString, clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T> T toObject(String jsonString, TypeReference<T> valueTypeRef) {
		if (StringUtils.isEmpty(jsonString)) {
			return null;
		}
		try {
			return getMapper().readValue(jsonString, valueTypeRef);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T> List<T> toList(String jsonString, Class<T> clazz) {
		if (StringUtils.isEmpty(jsonString)) {
			return null;
		}

		JavaType javaType = getMapper().getTypeFactory().constructParametricType(ArrayList.class, clazz);
		return toObject(jsonString, javaType);
	}
	
	public static <K,V> Map<K, V> toHashMap(String jsonString, Class<K> keyType, Class<V> valueType) {
		if (StringUtils.isEmpty(jsonString)) {
			return null;
		}
		JavaType javaType = getMapper().getTypeFactory().constructParametricType(HashMap.class, keyType,valueType);
		return toObject(jsonString, javaType);
	}
	
	public static <V> Map<String, V> toHashMap(String jsonString, Class<V> valueType) {
		return toHashMap(jsonString, String.class, valueType);
	}
	
	public static Map<String, Object> toHashMap(String jsonString) {
		return toHashMap(jsonString, String.class, Object.class);
	}

	
	@SuppressWarnings("unchecked")
	public static <T> T toObject(String jsonString, JavaType javaType) {
		if (StringUtils.isEmpty(jsonString)) {
			return null;
		}
		try {
			return (T) getMapper().readValue(jsonString, javaType);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static JsonNode selectJsonNode(String jsonString,String nodeName){
		JsonNode jsonNode = toJsonNode(jsonString);
		return nodeName == null ? jsonNode : jsonNode.get(nodeName);
	}
	
	public static JsonNode toJsonNode(String jsonString){
		try {
			JsonNode node = getMapper().readTree(jsonString);	
			return node;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static ArrayNode toJsonArrayNode(String jsonString){
		try {
			JsonNode node = getMapper().readTree(jsonString);	
			return (ArrayNode)node;
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
		if(StringUtils.isBlank(jsonString))return null;
		return getJsonNodeValue(selectJsonNode(jsonString, null), attrs);
	}
	
	public static <T> T toObject(JsonNode node,Class<T> clazz) {
		return toObject(node.toString(), clazz);
	}
	
	public static <T> List<T> toList(ArrayNode node,Class<T> clazz) {
		List<T> result = new ArrayList<>(node.size());
		for (int i = 0; i < node.size(); i++) {
			result.add(toObject(node.get(i), clazz));
		}
		return result;
	}
	
	/**
	 * 
	 * @param node
	 * @param attrs (e.g:info.user.id)
	 * @return
	 */
	public static String getJsonNodeValue(JsonNode node, String attrs) {  
		//ObjectNode,ArrayNode
        int index = attrs.indexOf(GlobalConstants.DOT);  
        JsonNode subNode = null;
        if (index == -1) {  
            if (node != null) {  
            	if(node instanceof ArrayNode) {
                	ArrayNode arrayNode = (ArrayNode) node;
                	subNode = arrayNode.isEmpty() ? null : arrayNode.get(0).get(attrs);
                }else {
                	subNode = node.get(attrs);
                }
            	
            	if(subNode == null)return null;
            	if(subNode instanceof ValueNode) {
            		return subNode.asText();
            	}
            	
                return subNode.toString();
            }  
            return null;  
        } else {  
            String s1 = attrs.substring(0, index);  
            String s2 = attrs.substring(index + 1);  
            if(node instanceof ArrayNode) {
            	ArrayNode arrayNode = (ArrayNode) node;
            	subNode = arrayNode.isEmpty() ? null : arrayNode.get(0).get(s1);
            }else {
            	subNode = node.get(s1);
            }
			return getJsonNodeValue(subNode, s2);  
        }  
    }  
	
	public static void main(String[] args) {
		String json = "{\"id\":1300,\"details\":{\"type\":\"catalog\",\"code\":\"monitor\",\"depts\":{\"code\":\"monitor\",\"children\":[{\"deptId\":\"00000030\",\"deptName\":\"事业部\"},{\"deptId\":\"00027675\",\"deptName\":\"中科云谷\"}]}}}";
		String value = getJsonNodeValue(json, "id");
		System.out.println("id:" + value);
		
		value = getJsonNodeValue(json, "details.type");
		System.out.println("details.type:" + value);
		
		value = getJsonNodeValue(json, "details.depts.children");
		System.out.println("details.depts.children:" + value);
		
	}
}