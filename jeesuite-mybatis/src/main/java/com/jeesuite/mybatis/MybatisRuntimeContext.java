package com.jeesuite.mybatis;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.ThreadLocalContext;
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


	private static final String CONTEXT_USER_ID_KEY = "_ctx_userId_";
	private static final String CONTEXT_TRANS_ON_KEY = "_ctx_trans_on_";
	private static final String CONTEXT_DATASOURCE_KEY = "_ctx_ds_";
	private static final String CONTEXT_TENANT_ID_KEY = "_ctx_tenantId_";
	
	public static void setCurrentUserId(Serializable userId){
		ThreadLocalContext.set(CONTEXT_USER_ID_KEY, userId);
	}
	
	public static void setTenantId(String tenantId){
		if(StringUtils.isBlank(tenantId))return;
		ThreadLocalContext.set(CONTEXT_TENANT_ID_KEY, tenantId);
	}
	
	public static String getContextParam(String paramName){
		if(StringUtils.isBlank(paramName))return null;
		if(CacheHandler.CURRENT_USER_CONTEXT_NAME.equals(paramName)){
			return getCurrentUserId();
		}
		return ThreadLocalContext.getStringValue(paramName);
	}
	
	public static void setContextParam(String name,String value){
		ThreadLocalContext.set(name, value);
	}
	
	public static void setTransactionalMode(boolean on){
		ThreadLocalContext.set(CONTEXT_TRANS_ON_KEY, String.valueOf(on));
		if(on){
			forceMaster();
		}
	}
	
	public static String getTransactionalMode(){
		return ThreadLocalContext.getStringValue(CONTEXT_TRANS_ON_KEY);
	}
	
	public static String getCurrentUserId(){
		return ThreadLocalContext.getStringValue(CONTEXT_USER_ID_KEY);
	}
	
	public static String getTenantId(){
		return ThreadLocalContext.getStringValue(CONTEXT_TENANT_ID_KEY);
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
		DataSourceContextVals dataSourceContextVals = ThreadLocalContext.get(CONTEXT_DATASOURCE_KEY);
		if(dataSourceContextVals == null){
			dataSourceContextVals = new DataSourceContextVals();
			ThreadLocalContext.set(CONTEXT_DATASOURCE_KEY, dataSourceContextVals);
		}
		return dataSourceContextVals;
	}
	
	public static void unset(){
		ThreadLocalContext.remove(CONTEXT_TRANS_ON_KEY);
	}
	
}
