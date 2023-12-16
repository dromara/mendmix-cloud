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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.model.DataPermItem;
import com.mendmix.mybatis.datasource.DataSourceContextVals;
import com.mendmix.mybatis.metadata.MapperMetadata;
import com.mendmix.mybatis.plugin.cache.CacheHandler;
import com.mendmix.mybatis.plugin.rewrite.SpecialPermType;
import com.mendmix.mybatis.plugin.rewrite.SqlRewriteStrategy;
import com.mendmix.mybatis.plugin.rewrite.UserPermissionProvider;
import com.mendmix.mybatis.plugin.rewrite.annotation.DataPermission;
import com.mendmix.spring.InstanceFactory;

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


	private static final String CONTEXT_FORCE_MASTER_KEY = "_ctx_force_master_";
	private static final String CONTEXT_DATASOURCE_KEY = "_ctx_ds_";
	private static final String CONTEXT_DATA_PROFILE_KEY = "_ctx_dataprofile_";
	private static final String CONTEXT_REWRITE_STRATEGY = "_ctx_rewrite_strategy_";
	private static final String CONTEXT_MAPPER_METADATA = "_ctx_mapper_metadata_";
	
	private static UserPermissionProvider  userPermissionProvider;
	
	static {
		userPermissionProvider = InstanceFactory.getInstance(UserPermissionProvider.class);
	}

	public static UserPermissionProvider getUserPermissionProvider() {
		if(userPermissionProvider != null)return userPermissionProvider;
		synchronized (MybatisRuntimeContext.class) {
			if(userPermissionProvider != null)return userPermissionProvider;
			userPermissionProvider = InstanceFactory.getInstance(UserPermissionProvider.class);
			if(userPermissionProvider == null) {
				userPermissionProvider = new UserPermissionProvider() {	
					@Override
					public List<DataPermItem> findUserPermissions(String userId) {
						return null;
					}
				};
			}
		}
		return userPermissionProvider;
	}

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

	
	public static void setIgnoreSqlRewrite(boolean ignore){
		getSqlRewriteStrategy().setIgnoreAny(ignore);
	}
	
	public static void setIgnoreTenant(boolean ignore){
		getSqlRewriteStrategy().setIgnoreTenant(ignore);
	}


	public static void setIgnoreSoftDeleteConditon(boolean ignore){
		getSqlRewriteStrategy().setIgnoreSoftDelete(ignore);
	}

	public static void setDataPermissionStrategy(DataPermission annotation) {
		getSqlRewriteStrategy().setDataPermission(annotation);
	}
	
	public static void setMapperMetadata(MapperMetadata mapperMetadata){
		ThreadLocalContext.set(CONTEXT_MAPPER_METADATA, mapperMetadata);
	}
	
	public static MapperMetadata getMapperMetadata(){
		return ThreadLocalContext.get(CONTEXT_MAPPER_METADATA);
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
	
	public static void forceUseMaster(){
		ThreadLocalContext.set(CONTEXT_FORCE_MASTER_KEY, String.valueOf(true));
		useMaster();
	}
	
	public static boolean isForceUseMaster(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_FORCE_MASTER_KEY));
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
	public static void useMaster(){
		DataSourceContextVals vals = MybatisRuntimeContext.getDataSourceContextVals();
		vals.master = true;
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
	
	public static void setDataPermissionValues(Map<String, String[]> valueMap,boolean append){
		if(append && ThreadLocalContext.exists(CONTEXT_DATA_PROFILE_KEY)) {
			Map<String, String[]> map = ThreadLocalContext.get(CONTEXT_DATA_PROFILE_KEY);
			map.putAll(valueMap);
		}else {
			if(valueMap == null) {
				ThreadLocalContext.remove(CONTEXT_DATA_PROFILE_KEY);
			}else {				
				ThreadLocalContext.set(CONTEXT_DATA_PROFILE_KEY,valueMap);
			}
		}
	}
	
	public static Map<String, String[]> getDataPermissionValues(){
		Map<String, String[]> map = ThreadLocalContext.get(CONTEXT_DATA_PROFILE_KEY);
		if(map != null)return map;
		List<DataPermItem> items = getUserPermissionProvider().findCurrentAllPermissions();
		if(items != null) {
			map = new HashMap<>(items.size());
			String[] values;
			for (DataPermItem item : items) {
				values = map.get(item.getKey());
				if (item.getValues() != null) {
					if(values == null) {
						values = item.getValues().toArray(new String[0]);
					}else {
						if(item.getValues().size() == 1) {
							String val = item.getValues().get(0);
							if(!ArrayUtils.contains(values, val)) {
								values = ArrayUtils.add(values, val);
							}
						}else {
							values = ArrayUtils.addAll(values, item.getValues().toArray(new String[0]));
						}
					}
				}else if(item.isAllMatch()) {
					values = new String[] {SpecialPermType._allValues.name()};
				}
				if(values == null) {
					values = new String[0];
				}else if(values.length > 1 && ArrayUtils.contains(values, SpecialPermType._allValues.name())) {
					values = new String[] {SpecialPermType._allValues.name()};
				}
				map.put(item.getKey(), values);
			}
			//
			if(!map.isEmpty() && map.values().stream().allMatch(arr -> arr.length > 0 && SpecialPermType._allValues.name().equals(arr[0]))) {
				getSqlRewriteStrategy().setHandleOwner(false);
			}
			ThreadLocalContext.set(CONTEXT_DATA_PROFILE_KEY, map);
		}
		return  map;
	}
	
}
