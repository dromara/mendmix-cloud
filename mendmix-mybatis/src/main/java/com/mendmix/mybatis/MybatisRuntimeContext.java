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
package com.mendmix.mybatis;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.model.AuthUser;
import com.mendmix.mybatis.datasource.DataSourceContextVals;
import com.mendmix.mybatis.plugin.cache.CacheHandler;
import com.mendmix.mybatis.plugin.rewrite.SqlRewriteStrategy;
import com.mendmix.mybatis.plugin.rewrite.annotation.DataPermission;

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
	private static final String CONTEXT_REWRITE_STRATEGY = "_ctx_rewrite_strategy_";
	
 	
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
	
	
	public static void setIgnoreSqlRewrite(boolean ignore){
		getSqlRewriteStrategy().setIgnoreAny(ignore);
	}
	
	public static void setIgnoreTenant(boolean ignore){
		getSqlRewriteStrategy().setIgnoreTenant(ignore);
	}


	public static void setIgnoreSoftDeleteConditon(boolean ignore){
		getSqlRewriteStrategy().setIgnoreSoftDelete(ignore);
	}
	
	public static void setIgnoreDataPermission(boolean ignore){
		getSqlRewriteStrategy().setIgnoreColumnPerm(ignore);
	}
	
	public static void setDataPermissionStrategy(DataPermission annotation) {
		getSqlRewriteStrategy().setDataPermission(annotation);
	}
	
	public static SqlRewriteStrategy getSqlRewriteStrategy() {
		SqlRewriteStrategy strategy = ThreadLocalContext.get(CONTEXT_REWRITE_STRATEGY);
		if(strategy == null) {
			strategy = new SqlRewriteStrategy(MybatisConfigs.DATA_PERM_ALL_MATCH_MODE_ENABLED, null);
			ThreadLocalContext.set(CONTEXT_REWRITE_STRATEGY, strategy);
		}
		return strategy;
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
	
	public static void addDataPermissionValues(String fieldName,String...fieldValues){
		Map<String, String[]> map = getDataPermissionValues();
		if(map == null){
			map = new HashMap<>(5);
			ThreadLocalContext.set(CONTEXT_DATA_PROFILE_KEY,map);
		}
		map.put(fieldName, fieldValues);
	}
	
	public static Map<String, String[]> getDataPermissionValues(){
		return ThreadLocalContext.get(CONTEXT_DATA_PROFILE_KEY);
	}
	

	/**
	 * 清理每一次数据库操作的上下文
	 */
	public static void unsetEveryTime(){
		ThreadLocalContext.remove(CONTEXT_DATASOURCE_KEY);
	}
	
}
