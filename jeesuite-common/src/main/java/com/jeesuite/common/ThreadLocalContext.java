package com.jeesuite.common;

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
	
	public static final String TENANT_ID_KEY = "_ctx_tenantId_";
	
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
	
	public static <T> T get(String key){
		if(context.get() == null)return null;
		return (T) context.get().get(key);
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
			//context.get().clear();
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
