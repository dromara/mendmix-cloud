/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeesuite.springweb.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.util.BeanUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年4月25日
 */
@SuppressWarnings({ "unchecked"})
public class ParameterUtils {

	public final static String CONTACT_STR = "&";
	public final static String EQUALS_STR = "=";
	public final static String SPLIT_STR = ",";
	
	public static final String JSON_SUFFIX = "}";
	public static final String JSON_PREFIX = "{";
	
	public static final String BRACKET_PREFIX = "[";
	public static final String BRACKET_SUFFIX = "]";
	
	public final static String PARAM_SIGN = "sign";
	public final static String PARAM_SIGN_TYPE = "signType";
	public final static String PARAM_DATA = "data";
	
	public static Map<String, Object> queryParamsToMap(String queryParams){
		Map<String, Object>  map = new HashMap<String, Object>();
		String[] paramSegs = StringUtils.split(queryParams, CONTACT_STR);
		String[] kv;
		for (String param : paramSegs) {
			kv = StringUtils.split(param,EQUALS_STR);
			if(kv.length == 1 || StringUtils.isBlank(kv[1]))continue;
			map.put(kv[0].trim(), kv[1].trim());
		}
		return map;
	}
	
	
	public static String mapToQueryParams(Map<String, Object> map,boolean sort){
		StringBuilder sb = new StringBuilder();
		List<String> keys = new ArrayList<>(map.keySet());
		if(sort){
			Collections.sort(keys);
		}
		for (String key : keys) {
			sb.append(key).append(EQUALS_STR).append(map.get(key)).append(CONTACT_STR);
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	public static String objectToSignContent(Object param){
		Map<String, Object> map = BeanUtils.beanToMap(param);
		return mapToSignContent(map);
	}
	
	
	public static String mapToSignContent(Map<String, Object> param){

		if(param == null || param.isEmpty())return null;
		StringBuilder sb = new StringBuilder();
		List<String> keys = new ArrayList<>(param.keySet());
		Collections.sort(keys);
		Object value;
		for (String key : keys) {
			if(PARAM_SIGN_TYPE.equals(key) || PARAM_SIGN.equals(key))continue;
			value = param.get(key);
			if(value == null || StringUtils.isBlank(value.toString()))continue;
			if(value instanceof Map){
				value = mapToSignContent((Map<String, Object>) value);
				if(value != null){
					value = JSON_PREFIX + value + JSON_SUFFIX;
				}
			}else if(value instanceof Iterable) {
        		StringBuilder sb1 = new StringBuilder();
        		sb1.append(BRACKET_PREFIX);
                Iterator<?> it = ((Iterable<?>) value).iterator();
                while (it.hasNext()) {
                	Object object = it.next();
                	if(BeanUtils.isSimpleDataType(object)){
                		sb1.append(object).append(SPLIT_STR);
                	}else{                		
                		sb1.append(JSON_PREFIX).append(objectToSignContent(object)).append(JSON_SUFFIX).append(SPLIT_STR);
                	}
                }
                if(sb1.length() == 1){
                	value = null;
                } else if(sb1.length() > 0){
                	sb1.deleteCharAt(sb1.length() - 1);
                	sb1.append(BRACKET_SUFFIX);
                	value = sb1.toString();
                }
            }
			if(value != null){
				sb.append(key).append(EQUALS_STR).append(value).append(CONTACT_STR);	
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}


}
