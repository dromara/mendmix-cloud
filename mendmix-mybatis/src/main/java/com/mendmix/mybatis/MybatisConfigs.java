/*
 * Copyright 2016-2018 www.mendmix.com.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

import com.mendmix.common.util.ResourceUtils;
import com.mendmix.mybatis.datasource.DataSourceConfig;
import com.mendmix.mybatis.datasource.DataSoureConfigHolder;
import com.mendmix.mybatis.datasource.DatabaseType;
import com.mendmix.mybatis.plugin.cache.CacheHandler;
import com.mendmix.mybatis.plugin.rwseparate.RwRouteHandler;
import com.mendmix.mybatis.plugin.shard.TableShardingHandler;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class MybatisConfigs {
	
	public static final String DB_TYPE = "mendmix.dbType";
	public static final String CACHE_ENABLED = "mendmix.mybatis.cache.enabled";
	public static final String CACHE_EXPIRE_SECONDS = "mendmix.mybatis.cache.expireSeconds";
	public static final String TENANT_ENABLED = "mendmix.mybatis.tenant.enabled";
	public static final String TENANT_IGNORE_USER_TYPE = "mendmix.mybatis.tenant.ignoreUserType";
	public static final String TENANT_COLUMN_NAME = "mendmix.mybatis.tenant.columnName";
	public static final String TABLE_SHARDING_ENABLED = "mendmix.mybatis.tableShard.enabled";
	public static final String INTERCEPTOR_HANDLERCLASS = "mendmix.mybatis.interceptorHandlerClass";
	public static final String SOFT_DELETE_COLUMN_NAME = "mendmix.mybatis.softDelete.columnName";
	public static final String SOFT_DELETE_FALSE_VALUE = "mendmix.mybatis.softDelete.falseValue";
	public static final String OWNER_COLUMN_NAME = "mendmix.mybatis.createBy.columnName";
	public static final String DEPT_COLUMN_NAME = "mendmix.mybatis.department.columnName";
	public static final String ORG_BASE_PERM_KEY = "mendmix.mybatis.currentOrgPermKey";
	
	public static final boolean DATA_PERM_ALL_MATCH_MODE_ENABLED = ResourceUtils.getBoolean("application.mybatis.dataPermssion.allMatchMode.enabled",true);
	
	
	private static Map<String, Properties> groupProperties = new HashMap<>();
	
	
	public static String getProperty(String group,String key,String defaultValue){
		if(!groupProperties.containsKey(group)) {
			synchronized (groupProperties) {
				String prefix = DataSourceConfig.DEFAULT_GROUP_NAME.equals(group) ? "mendmix.mybatis" : group + ".mendmix.mybatis";
				MybatisConfigs.addProperties(group, ResourceUtils.getAllProperties(prefix));
			}
		}
		return groupProperties.get(group).getProperty(key, defaultValue);
	}
	
	private static void addProperties(String group,Properties properties){
		if(!DataSourceConfig.DEFAULT_GROUP_NAME.equals(group)) {
			String prefix = group + ".";
			Properties _properties = new Properties();
			properties.forEach( (k,v) -> {
				if(v != null) {
					_properties.put(k.toString().replace(prefix, ""),v);
				}
			} );
			properties = _properties;
		}
		groupProperties.put(group, properties);
	}
	
	public static boolean getBoolean(String group,String key,boolean defaultValue){
		return Boolean.parseBoolean(getProperty(group, key, String.valueOf(defaultValue)));
	}
	
	public static String getDbType(String group){
		return getProperty(group,DB_TYPE, DatabaseType.mysql.name()).toLowerCase();
	}
	
	public static boolean isCacheEnabled(String group) {
		return getBoolean(group,CACHE_ENABLED, false);
	}
	
	public static boolean isTableShardEnabled(String group) {
		return getBoolean(group,TABLE_SHARDING_ENABLED, false);
	}
	
	public static String getTenantColumnName(String group) {
		return getProperty(group,TENANT_COLUMN_NAME,null);
	}
	
	public static String getSoftDeleteColumn(String group) {
		return getProperty(group,SOFT_DELETE_COLUMN_NAME,null);
	}
	
	public static String getSoftDeletedFalseValue(String group) {
		return getProperty(group,SOFT_DELETE_FALSE_VALUE,"0");
	}
	
	public static String getCurrentOrgPermKey(String group){
		return getProperty(group,ORG_BASE_PERM_KEY, "organization");
	}
	
	public static String getOwnerColumnName(String group) {
		return getProperty(group,OWNER_COLUMN_NAME,null);
	}
	
	public static String getDeptColumnName(String group) {
		return getProperty(group,DEPT_COLUMN_NAME,null);
	}
	
	public static boolean isColumnSharddingTenant(String group) {
		return getBoolean(group,TENANT_ENABLED, false);
	}
	
	public static boolean isDataPermissionEnabled(String group) {
		return getBoolean(group,"mendmix.mybatis.dataPermission.enabled", false);
	}
	
	public static boolean isSchameSharddingTenant(String group) {
		return DataSoureConfigHolder.containsTenantConfig(group);
	}
	
	public static String[] getHandlerNames(String group){
        List<String> hanlders = new ArrayList<>();
        String customHandlers = getProperty(group, INTERCEPTOR_HANDLERCLASS, null);
		if(!org.apache.commons.lang3.StringUtils.isBlank(customHandlers)){
			String[] customHanlderClass = StringUtils.tokenizeToStringArray(customHandlers, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
			hanlders.addAll(Arrays.asList(customHanlderClass));
		}

		if (isCacheEnabled(group)) {
			hanlders.add(CacheHandler.NAME);
		}
		
		if(isTableShardEnabled(group)) {
			hanlders.add(TableShardingHandler.NAME);
		}
        //
		if (DataSoureConfigHolder.containsSlaveConfig()) {
			hanlders.add(RwRouteHandler.NAME);
		}
		
		return hanlders.toArray(new String[0]);
	}
}
