package com.jeesuite.common.util;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.jeesuite.common.json.JsonMapper;

public class JsonUtils {

	private static JsonMapper jsonMapper = JsonMapper.getDefault();

	public static ObjectMapper getMapper() {
		return jsonMapper.getMapper();
	}

	public static String toJson(Object object) {
		return jsonMapper.toJson(object);
	}

	/**
	 * 不含值为null的属性
	 * 
	 * @param object
	 * @return
	 */
	public static String toJsonIgnoreNullField(Object object) {
		return JsonMapper.nonNullMapper().toJson(object);
	}

	/**
	 * 转换成格式化的json字符串
	 * 
	 * @param object
	 * @return
	 */
	public static String toPrettyJson(Object object) {
		try {
			return getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T toObject(String jsonString, Class<T> clazz) {
		return jsonMapper.toObject(jsonString, clazz);
	}

	public static <T> T toObject(String jsonString, TypeReference<T> valueTypeRef) {
		try {
			JsonParser parser = jsonMapper.getMapper().createParser(jsonString);
			return jsonMapper.getMapper().readValue(parser, valueTypeRef);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> List<T> toList(String jsonString, Class<T> clazz) {
		return jsonMapper.toList(jsonString, clazz);
	}

	public static <K, V> Map<K, V> toHashMap(String jsonString, Class<K> keyType, Class<V> valueType) {
		return jsonMapper.toHashMap(jsonString, keyType, valueType);
	}

	public static <V> Map<String, V> toHashMap(String jsonString, Class<V> valueType) {
		return jsonMapper.toHashMap(jsonString, String.class, valueType);
	}

	public static JsonNode getNode(String jsonString, String nodeName) {
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
	 * @param attrs
	 *            (e.g:info.user.id)
	 * @return
	 */
	public static String getJsonNodeValue(String jsonString, String attrs) {
		return getJsonNodeValue(getNode(jsonString, null), attrs);
	}

	/**
	 * 
	 * @param node
	 * @param attrs
	 *            (e.g:info.user.id)
	 * @return
	 */
	public static String getJsonNodeValue(JsonNode node, String attrs) {
		int index = attrs.indexOf('.');
		JsonNode subNode = null;
		if (index == -1) {
			if (node != null) {
				if (node instanceof ArrayNode) {
					ArrayNode arrayNode = (ArrayNode) node;
					subNode = arrayNode.isEmpty() ? null : arrayNode.get(0).get(attrs);
				} else {
					subNode = node.get(attrs);
				}

				if (subNode == null)
					return null;
				if (subNode instanceof ValueNode) {
					return subNode.asText();
				}

				return subNode.toString();
			}
			return null;
		} else {
			String s1 = attrs.substring(0, index);
			String s2 = attrs.substring(index + 1);
			if (node instanceof ArrayNode) {
				ArrayNode arrayNode = (ArrayNode) node;
				subNode = arrayNode.isEmpty() ? null : arrayNode.get(0).get(s1);
			} else {
				subNode = node.get(s1);
			}
			return getJsonNodeValue(subNode, s2);
		}
	}

}