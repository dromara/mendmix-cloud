package com.jeesuite.mybatis;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.mybatis.datasource.DataSourceContextVals;
import com.jeesuite.mybatis.plugin.cache.CacheHandler;

/**
 * 
 * <br>
 * Class Name   : MybatisRuntimeContext
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年2月14日
 */
public class MybatisRuntimeContext {


	private static ThreadLocal<Map<String, Object>> context = new ThreadLocal<>();
	private static final String CONTEXT_USER_ID_KEY = "_context_user_id_";
	private static final String CONTEXT_TRANS_ON_KEY = "_context_trans_on_";
	private static final String CONTEXT_DATASOURCE_KEY = "_context_ds_";
	
	public static void setCurrentUserId(Serializable userId){
		if(context.get() == null){
			context.set(new HashMap<>(5));
		}
		context.get().put(CONTEXT_USER_ID_KEY, userId);
	}
	
	public static String getContextParam(String paramName){
		if(StringUtils.isBlank(paramName))return null;
		if(CacheHandler.CURRENT_USER_CONTEXT_NAME.equals(paramName)){
			return getCurrentUserId();
		}
		return getStringValue(paramName);
	}
	
	public static void setContextParam(String name,String value){
		if(context.get() == null){
			context.set(new HashMap<>(5));
		}
		context.get().put(name, value);
	}
	
	public static void setTransactionalMode(boolean on){
		if(context.get() == null){
			context.set(new HashMap<>(3));
		}
		context.get().put(CONTEXT_TRANS_ON_KEY, String.valueOf(on));
		if(on){
			forceMaster();
		}
	}
	
	public static String getTransactionalMode(){
		return getStringValue(CONTEXT_TRANS_ON_KEY);
	}
	
	public static String getCurrentUserId(){
		return getStringValue(CONTEXT_USER_ID_KEY);
	}
	
	public static boolean isTransactionalOn(){
		return Boolean.parseBoolean(getStringValue(CONTEXT_TRANS_ON_KEY));
	}
	
	public static boolean isEmpty(){
		return context.get() == null || context.get().isEmpty();
	}
	
	/**
	 * 设置是否使用从库
	 * 
	 * @param useSlave
	 */
	public static void useSlave(boolean useSlave) {
		DataSourceContextVals vals = MybatisRuntimeContext.getDataSourceContextVals();
		vals.userSlave = useSlave;
	}
	
	/**
	 * 设置强制使用master库
	 */
	public static void forceMaster(){
		DataSourceContextVals vals = MybatisRuntimeContext.getDataSourceContextVals();
		vals.forceMaster = true;
	}
	
	/**
	 * 判断是否强制使用一种方式
	 * 
	 * @return
	 */
	public static boolean isForceUseMaster() {
		return MybatisRuntimeContext.getDataSourceContextVals().forceMaster;
	}
	
	public static DataSourceContextVals getDataSourceContextVals(){
		DataSourceContextVals dataSourceContextVals = null;
		if(context.get() != null){
			dataSourceContextVals = (DataSourceContextVals) context.get().get(CONTEXT_DATASOURCE_KEY);
		}
		if(dataSourceContextVals == null){
			if(context.get() == null){
				context.set(new HashMap<>(3));
			}
			dataSourceContextVals = new DataSourceContextVals();
			context.get().put(CONTEXT_DATASOURCE_KEY, dataSourceContextVals);
		}
		return dataSourceContextVals;
	}
	
	
	public static void unset(){
		if(context.get() != null){
			context.get().clear();
		}
	}
	
	private static String getStringValue(String key){
		if(context.get() == null)return null;
		return Objects.toString(context.get().get(key), null);
	}
}
