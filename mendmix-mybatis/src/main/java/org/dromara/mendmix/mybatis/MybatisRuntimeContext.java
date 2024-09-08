/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.mybatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.model.DataPermItem;
import org.dromara.mendmix.mybatis.datasource.DataSourceConfig;
import org.dromara.mendmix.mybatis.datasource.DataSourceContextVals;
import org.dromara.mendmix.mybatis.plugin.CurrentUserIdResolver;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.rewrite.DataPermissionStrategy;
import org.dromara.mendmix.mybatis.plugin.rewrite.UserPermissionProvider;
import org.dromara.mendmix.mybatis.plugin.rewrite.annotation.DataPermission;
import org.dromara.mendmix.spring.InstanceFactory;

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
	private static final String CONTEXT_FORCE_MASTER = "_ctx_force_master_";
	private static final String CONTEXT_DATASOURCE_KEY = "_ctx_ds_";
	private static final String CONTEXT_DATA_PROFILE_KEY = "_ctx_dataprofile_";
	private static final String CONTEXT_DATA_PERM_STRATEGY = "_ctx_dataperm_strategy_";
	private static final String CONTEXT_IGNORE_ANY = "_ctx_ignore_any_";
	private static final String CONTEXT_IGNORE_RWROUTE = "_ctx_ignore_rwroute_";
	private static final String CONTEXT_IGNORE_REWRITE = "_ctx_ignore_rewrite_";
	private static final String CONTEXT_IGNORE_DATA_PERM = "_ctx_ignore_dataperm_";
	private static final String CONTEXT_IGNORE_SOFT_DELETE = "_ctx_ignore_softdel_";
	private static final String CONTEXT_IGNORE_CACHE = "_ctx_ignore_cache_";
	private static final String CONTEXT_IGNORE_TABLE_SHARDING = "_ctx_ignore_tableSharding_";
	private static final String CONTEXT_IGNORE_LOGGING_CHANGED = "_ctx_ignore_logging_changed_";
	private static final String CONTEXT_REWRITE_TABLE_RULES = "_ctx_rewrite_table_rules_";
	private static final String CONTEXT_DATASOURCE_GROUP = "_ctx_datasource_group_";
	private static final String CONTEXT_IGNORE_OPER_PROTECT = "_ctx_ignore_operProtect_";
	private static final String CONTEXT_MAPPER_INVOCATION_VALS = "_ctx_mapper_invocationVals_";
	
	public static final String PARAM_CONTEXT_NAME = "__param_cxt_name";
	public static final String ATTR_VALUE_CONTEXT_NAME = "__attrval_cxt_name:%s:%s";
	public static final String DYNA_LIST_COUNT_CONTEXT_NAME = "__dynaListCount_cxt_name:%s";
	
	private static CurrentUserIdResolver currentUserIdResolver;
	private static UserPermissionProvider userPermissionProvider;
	static {
		if(InstanceFactory.isInitialized()) {			
			currentUserIdResolver = InstanceFactory.getInstance(CurrentUserIdResolver.class);
			userPermissionProvider = InstanceFactory.getInstance(UserPermissionProvider.class);
		}
	}
	
	public static CurrentUserIdResolver currentUserIdResolver() {
		if(currentUserIdResolver != null)return currentUserIdResolver;
		synchronized (MybatisRuntimeContext.class) {
			currentUserIdResolver = InstanceFactory.getInstance(CurrentUserIdResolver.class);
			if(currentUserIdResolver == null) {
				currentUserIdResolver = new CurrentUserIdResolver() {
					@Override
					public String resolve(AuthUser currentUser) {
						return currentUser.getId();
					}
				};
			}
		}
		return currentUserIdResolver;
	}
	
	public static UserPermissionProvider userPermissionProvider() {
		if(userPermissionProvider != null)return userPermissionProvider;
		synchronized (MybatisRuntimeContext.class) {
			userPermissionProvider = InstanceFactory.getInstance(UserPermissionProvider.class);
		}
		return userPermissionProvider;
	}



	public static String getCurrentTenant() {
    	String tenantId = CurrentRuntimeContext.getTenantId(false);
    	return tenantId;
    }
	
	public static String getCurrentUserId() {
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
    	return currentUser == null ? null : currentUserIdResolver().resolve(currentUser);
    }
	
	public static void setContextParam(String name,String value){
		ThreadLocalContext.set(name, value);
	}

	public static void setDatasourceGroup(String group) {
		if(group == null) {
			ThreadLocalContext.remove(CONTEXT_DATASOURCE_GROUP);
		}else {			
			ThreadLocalContext.set(CONTEXT_DATASOURCE_GROUP, group);
		}
	}
	
	public static String getDataSourceGroup() {
		if(ThreadLocalContext.exists(CONTEXT_DATASOURCE_GROUP)) {
			return ThreadLocalContext.getStringValue(CONTEXT_DATASOURCE_GROUP);
		}
		return StringUtils.defaultString(MybatisRuntimeContext.getDataSourceContextVals().group,DataSourceConfig.DEFAULT_GROUP_NAME);
	}
	
	public static void setTransactionalMode(boolean on){
		ThreadLocalContext.set(CONTEXT_TRANS_ON_KEY, String.valueOf(on));
		if(on){
			forceUseMaster();
		}
	}
	
	public static String getTransactionalMode(){
		return ThreadLocalContext.getStringValue(CONTEXT_TRANS_ON_KEY);
	}
	
	public static boolean isTransactionalOn(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_TRANS_ON_KEY));
	}
	
	
	public static void setIgnoreTenant(boolean ignore){
		ThreadLocalContext.set(CustomRequestHeaders.HEADER_IGNORE_TENANT, String.valueOf(ignore));
	}
	
	public static void setIgnoreTenantMode(){
		ThreadLocalContext.set(CustomRequestHeaders.HEADER_IGNORE_TENANT, Boolean.TRUE.toString());
	}
	
	public static boolean isIgnoreTenantMode(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CustomRequestHeaders.HEADER_IGNORE_TENANT));
	}
	
	public static void setIgnoreSqlRewrite(boolean ignore){
		ThreadLocalContext.set(CONTEXT_IGNORE_REWRITE, String.valueOf(ignore));
	}
	
	public static void setIgnoreAny(boolean ignore){
		ThreadLocalContext.set(CONTEXT_IGNORE_ANY, String.valueOf(ignore));
	}
	
	public static boolean isIgnoreAny(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_IGNORE_ANY));
	}
	
	public static void setIgnoreTableSharding(boolean ignore){
		ThreadLocalContext.set(CONTEXT_IGNORE_TABLE_SHARDING, String.valueOf(ignore));
	}
	
	public static boolean isIgnoreTableSharding(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_IGNORE_TABLE_SHARDING));
	}
	
	public static boolean isIgnoreSqlRewrite(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_IGNORE_REWRITE));
	}
	
	public static void setIgnoreCache(boolean ignore){
		ThreadLocalContext.set(CONTEXT_IGNORE_CACHE, String.valueOf(ignore));
	}
	
	public static boolean isIgnoreCache(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_IGNORE_CACHE));
	}
	
	public static void setIgnoreSoftDeleteConditon(boolean ignore){
		ThreadLocalContext.set(CONTEXT_IGNORE_SOFT_DELETE, String.valueOf(ignore));
	}
	
	public static boolean isIgnoreSoftDeleteConditon(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_IGNORE_SOFT_DELETE));
	}
	
	public static void setIgnoreLoggingDataChange(boolean ignore){
		ThreadLocalContext.set(CONTEXT_IGNORE_LOGGING_CHANGED, String.valueOf(ignore));
	}
	
	public static boolean isIgnoreLoggingDataChange(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_IGNORE_LOGGING_CHANGED));
	}
	
	public static boolean isIgnoreRwRoute(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_IGNORE_RWROUTE));
	}
	
	public static void setIgnoreRwRoute(boolean ignore){
		ThreadLocalContext.set(CONTEXT_IGNORE_RWROUTE, String.valueOf(ignore));
	}
	
	public static void setIgnoreDataPermission(boolean ignore){
		ThreadLocalContext.set(CONTEXT_IGNORE_DATA_PERM, String.valueOf(ignore));
	}
	
	public static boolean isIgnoreDataPermission(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_IGNORE_DATA_PERM));
	}
	
	public static void setDataPermissionStrategy(DataPermissionStrategy strategy) {
		ThreadLocalContext.set(CONTEXT_DATA_PERM_STRATEGY, strategy);
	}
	
	public static void setDataPermissionStrategy(DataPermission annotation) {
		ThreadLocalContext.set(CONTEXT_DATA_PERM_STRATEGY, new DataPermissionStrategy(annotation));
	}
	
	public static DataPermissionStrategy getDataPermissionStrategy() {
		DataPermissionStrategy strategy = ThreadLocalContext.get(CONTEXT_DATA_PERM_STRATEGY);
		if(strategy == null) {
			if(MybatisConfigs.DATA_PERM_ALL_MATCH_MODE_ENABLED) {
				strategy = new DataPermissionStrategy(true, null);
			}
			if(strategy != null) {
				ThreadLocalContext.set(CONTEXT_DATA_PERM_STRATEGY, strategy);
			}
		}
		return strategy;
	}
	
	public static void setTenantDataSourceKey(String dsKey) {
		CurrentRuntimeContext.setTenantDataSourceKey(dsKey);
	}
	
	public static String getTenantDataSourceKey() {
		return CurrentRuntimeContext.getTenantDataSourceKey();
	}
	
	public static void setIgnoreOperProtect(boolean ignore){
		ThreadLocalContext.set(CONTEXT_IGNORE_OPER_PROTECT, String.valueOf(ignore));
	}
	
	public static boolean isIgnoreOperProtect(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_IGNORE_OPER_PROTECT));
	}
	
	public static boolean isEmpty(){
		return ThreadLocalContext.isEmpty();
	}
	
	
	public static void forceUseMaster(){
		ThreadLocalContext.set(CONTEXT_FORCE_MASTER, Boolean.TRUE.toString());
		useMaster();
	}
	
	public static boolean isForceUseMaster(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_FORCE_MASTER));
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
		if(isForceUseMaster())return true;
		Boolean master = MybatisRuntimeContext.getDataSourceContextVals().master;
		return master == null ? true : master;
	}
	
	public static void setOnceContextVal(OnceContextVal onceContextVal){
		ThreadLocalContext.set(CONTEXT_MAPPER_INVOCATION_VALS, onceContextVal);
	}
	
	public static OnceContextVal getOnceContextVal(){
		return ThreadLocalContext.get(CONTEXT_MAPPER_INVOCATION_VALS);
	}
	
	public static DataSourceContextVals getDataSourceContextVals(){
		DataSourceContextVals dataSourceContextVals = ThreadLocalContext.get(CONTEXT_DATASOURCE_KEY);
		if(dataSourceContextVals == null){
			dataSourceContextVals = new DataSourceContextVals();
			if(isTransactionalOn())dataSourceContextVals.master = true;
			ThreadLocalContext.set(CONTEXT_DATASOURCE_KEY, dataSourceContextVals);
		}
		dataSourceContextVals.group = MybatisRuntimeContext.getDataSourceGroup();
		dataSourceContextVals.tenantId = MybatisRuntimeContext.getCurrentTenant();
		dataSourceContextVals.tenantDataSourceKey = getTenantDataSourceKey();
		return dataSourceContextVals;
	}
	
	public static void addDataPermissionValues(String fieldName,String...fieldValues){
		Map<String, String[]> map = ThreadLocalContext.get(CONTEXT_DATA_PROFILE_KEY);
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
 
	
	public static Map<String, String[]> getDataPermissionValues(String...groups){
		Map<String, String[]> map = ThreadLocalContext.get(CONTEXT_DATA_PROFILE_KEY);
		if(map != null)return map;
		List<DataPermItem> items = userPermissionProvider.findCurrentAllPermissions();
		if(items != null) {
			map = new HashMap<>(items.size());
			String[] values;
			for (DataPermItem item : items) {
				if(groups != null && !ArrayUtils.contains(groups, item.getGroupName())) {
					continue;
				}
				if(item.isAll()) {
					values = new String[] {DeptPermType._ALL_.name()};
				}else if (item.getValues() != null && item.getValues().size() > 0) {
					//多个合并的情况
					values = map.get(item.getKey());
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
				}else {
					continue;
				}
				//如果存在全部
                if(values.length > 1 && ArrayUtils.contains(values, DeptPermType._ALL_.name())) {
					values = new String[] {DeptPermType._ALL_.name()};
				}
				map.put(item.getKey(), values);
			}
			//
			if(!map.isEmpty() && map.values().stream().allMatch(arr -> arr.length > 0 && DeptPermType._ALL_.name().equals(arr[0]))) {
				DataPermissionStrategy.updateHandleOwner(false);
			}
			ThreadLocalContext.set(CONTEXT_DATA_PROFILE_KEY, map);
		}
		return  map;
	}
	
	public static Map<String, String> getRewriteTableNameRules(){
		return ThreadLocalContext.get(CONTEXT_REWRITE_TABLE_RULES);
	}
	
	public static void setRewriteTableNameRules(Map<String, String> rules) {
		if(rules == null) {
			if(ThreadLocalContext.exists(CONTEXT_REWRITE_TABLE_RULES)) {
				ThreadLocalContext.remove(CONTEXT_REWRITE_TABLE_RULES);
			}
			return;
		}
		ThreadLocalContext.set(CONTEXT_REWRITE_TABLE_RULES, rules);
	}
	
	public static void unsetOnceContext() {
		DataSourceContextVals dataSourceContextVals = ThreadLocalContext.get(CONTEXT_DATASOURCE_KEY);
		if(dataSourceContextVals != null){
			dataSourceContextVals.group = null;
			dataSourceContextVals.master = null;
		}
		ThreadLocalContext.remove(CONTEXT_IGNORE_OPER_PROTECT);
	}
	
}
