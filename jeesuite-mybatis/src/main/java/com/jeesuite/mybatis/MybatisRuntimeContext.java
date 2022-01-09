package com.jeesuite.mybatis;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.ThreadLocalContext;
import com.jeesuite.common.model.AuthUser;
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


	private static final String CONTEXT_TRANS_ON_KEY = "_ctx_trans_on_";
	private static final String CONTEXT_DATASOURCE_KEY = "_ctx_ds_";
	private static final String CONTEXT_DATA_PROFILE_KEY = "_ctx_dataprofile_";
	
 	
	public static String getContextParam(String paramName){
		if(StringUtils.isBlank(paramName))return null;
		if(CacheHandler.CURRENT_USER_CONTEXT_NAME.equals(paramName)){
			AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
			return currentUser == null ? null : currentUser.getName();
		}
		return ThreadLocalContext.getStringValue(paramName);
	}
	
	public static void setContextParam(String name,String value){
		ThreadLocalContext.set(name, value);
	}

	
	public static void setTransactionalMode(boolean on){
		ThreadLocalContext.set(CONTEXT_TRANS_ON_KEY, String.valueOf(on));
		if(on){
			userMaster();
		}
	}
	
	public static String getTransactionalMode(){
		return ThreadLocalContext.getStringValue(CONTEXT_TRANS_ON_KEY);
	}
	
	public static boolean isTransactionalOn(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_TRANS_ON_KEY));
	}
	
	public static boolean isEmpty(){
		return ThreadLocalContext.isEmpty();
	}
	
	/**
	 * 设置是否使用从库
	 * 
	 * @param useSlave
	 */
	public static void useSlave() {
		DataSourceContextVals vals = MybatisRuntimeContext.getDataSourceContextVals();
		vals.master = false;
	}
	
	/**
	 * 设置强制使用master库
	 */
	public static void userMaster(){
		DataSourceContextVals vals = MybatisRuntimeContext.getDataSourceContextVals();
		vals.master = true;
	}
	
	public static boolean isRwRouteAssigned() {
		return MybatisRuntimeContext.getDataSourceContextVals().master != null;
	}
	
	public static boolean isUseMaster() {
		Boolean master = MybatisRuntimeContext.getDataSourceContextVals().master;
		return master == null ? true : master;
	}
	
	public static DataSourceContextVals getDataSourceContextVals(){
		DataSourceContextVals dataSourceContextVals = ThreadLocalContext.get(CONTEXT_DATASOURCE_KEY);
		if(dataSourceContextVals == null){
			dataSourceContextVals = new DataSourceContextVals();
			dataSourceContextVals.tenantId = CurrentRuntimeContext.getTenantId();
			ThreadLocalContext.set(CONTEXT_DATASOURCE_KEY, dataSourceContextVals);
		}
		return dataSourceContextVals;
	}
	
	public static void addDataProfileMappings(String fieldName,String...fieldValues){
		Map<String, String[]> map = getDataProfileMappings();
		if(map == null){
			map = new HashMap<>(5);
			ThreadLocalContext.set(CONTEXT_DATA_PROFILE_KEY,map);
		}
		map.put(fieldName, fieldValues);
	}
	
	public static Map<String, String[]> getDataProfileMappings(){
		return ThreadLocalContext.get(CONTEXT_DATA_PROFILE_KEY);
	}
	

	/**
	 * 清理每一次数据库操作的上下文
	 */
	public static void unsetEveryTime(){
		ThreadLocalContext.remove(CONTEXT_DATASOURCE_KEY);
	}
	
}
