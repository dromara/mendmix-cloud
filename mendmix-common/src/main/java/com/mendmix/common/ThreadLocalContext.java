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
package com.mendmix.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * 
 * <br>
 * Class Name   : ThreadLocalContext
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年6月30日
 */
public class ThreadLocalContext {

	private static ThreadLocal<Map<String, Object>> context = new ThreadLocal<>();
	
	public static String REQUEST_CONTEXT_KEY = "_request_context";
	
	public static void set(String key,Object value){
		if(value == null)return;
		getContextMap().put(key, value);
	}
	
	public static String getStringValue(String key){
		if(context.get() == null)return null;
		return Objects.toString(context.get().get(key), null);
	}
	
	public static boolean exists(String key){
		if(context.get() == null)return false;
		return context.get().containsKey(key);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T get(String key){
		if(context.get() == null)return null;
		return (T) context.get().get(key);
	}
	
	public static <T> T get(String key,T defaultVal){
		T value = get(key);
		return value == null ? defaultVal : value;
	}
	
	public static void remove(String...keys){
		if(context.get() == null)return;
		for (String key : keys) {
			context.get().remove(key);
		}
	}
	
	public static boolean isEmpty(){
		return context.get() == null || context.get().isEmpty();
	}
	
	public static void unset(){
		if(context.get() != null){
			context.get().clear();
			context.remove();
		}
	}
	
	private static Map<String, Object> getContextMap(){
		if(context.get() == null){
			context.set(new HashMap<>());
		}
		return context.get();
	}

}
