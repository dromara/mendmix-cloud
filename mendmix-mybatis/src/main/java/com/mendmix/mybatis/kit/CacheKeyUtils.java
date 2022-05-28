/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.mybatis.kit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.util.BeanUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月1日
 */
public class CacheKeyUtils {
	
	public final static List<String> MYBATIS_DEFAULT_LIST_NAMES = Arrays.asList("arg0","collection","list");
	public final static String CONTACT_STR = "&";
	public final static String EQUALS_STR = "=";
	public final static String SPLIT_STR = ",";
	
	public static final String JSON_SUFFIX = "}";
	public static final String JSON_PREFIX = "{";
	
	public static final String BRACKET_PREFIX = "[";
	public static final String BRACKET_SUFFIX = "]";
	
	@SuppressWarnings("unchecked")
	public static String objcetToString(Object obj){
		if(obj == null)return StringUtils.EMPTY;
        if(BeanUtils.isSimpleDataType(obj)){
			return obj.toString();
		}
        if(obj instanceof Iterable) {
    		StringBuilder sb = new StringBuilder();
    		sb.append(BRACKET_PREFIX);
            Iterator<?> it = ((Iterable<?>) obj).iterator();
            while (it.hasNext()) {
            	Object object = it.next();
            	if(BeanUtils.isSimpleDataType(object)){
            		sb.append(object).append(SPLIT_STR);
            	}else{                		
            		sb.append(JSON_PREFIX).append(objcetToString(object)).append(JSON_SUFFIX).append(SPLIT_STR);
            	}
            }
            if(sb.length() == 1){
            	return StringUtils.EMPTY;
            } else if(sb.length() > 0){
            	sb.deleteCharAt(sb.length() - 1);
            	sb.append(BRACKET_SUFFIX);
            	return sb.toString();
            }
        }
        
		Map<String, Object> param = null;
		if(obj instanceof Map){
        	param = (Map<String, Object>) obj;
        }else{
        	param = BeanUtils.beanToMap(obj,true);
        }
		if(param == null || param.isEmpty())return null;
		StringBuilder sb = new StringBuilder();
		List<String> keys = new ArrayList<>(param.keySet());
		
		 boolean isMybatisDefaultList = keys.size() == 3 && keys.containsAll(MYBATIS_DEFAULT_LIST_NAMES);
		Collections.sort(keys);
		Object value;
		for (String key : keys) {
			value = param.get(key);
			if(value == null || StringUtils.isBlank(value.toString()))continue;
			if(value instanceof Map){
				value = objcetToString(value);
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
                		sb1.append(JSON_PREFIX).append(objcetToString(object)).append(JSON_SUFFIX).append(SPLIT_STR);
                	}
                }
                if(sb1.length() == 1){
                	value = null;
                } else if(sb1.length() > 0){
                	sb1.deleteCharAt(sb1.length() - 1);
                	sb1.append(BRACKET_SUFFIX);
                	value = sb1.toString();
                }
            }else if(!BeanUtils.isSimpleDataType(value)){
            	value = JSON_PREFIX + objcetToString(value) + JSON_SUFFIX;
            }
			if(value != null){
				sb.append(key).append(EQUALS_STR).append(value).append(CONTACT_STR);	
			}
			
			if(isMybatisDefaultList)break;
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

}
