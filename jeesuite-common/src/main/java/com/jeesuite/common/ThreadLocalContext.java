package com.jeesuite.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;


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
	
	private static final String _ID = "_thread_id_";
	public static final String REQUEST_KEY = "_ctx_request_";
	public static final String RESPONSE_KEY = "_ctx_response_";
	public static final String CURRENT_USER_KEY = "_ctx_current_user_";
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
	
	public static boolean isExpired(){
		if(isEmpty())return false;
		String threadId = getStringValue(_ID);
		if(threadId == null)return false;
		String id = buildThreadId();
		return !StringUtils.equals(threadId, id);
	}
	
	public static String getId(){
		return getStringValue(_ID);
	}
	
	public static void unset(){
		if(context.get() != null){
			context.remove();
		}
	}
	
	private static Map<String, Object> getContextMap(){
		if(context.get() == null){
			context.set(new HashMap<>());
			String id = buildThreadId();
			set(_ID, id);
		}
		return context.get();
	}
	
	private static String buildThreadId(){
		return new StringBuilder()
				   .append(Thread.currentThread().getName())
				   .append(Thread.currentThread().getId()).toString();
	}
	
	
}
