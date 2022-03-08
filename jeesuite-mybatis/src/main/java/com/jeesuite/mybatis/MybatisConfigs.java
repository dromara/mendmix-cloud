/*
 * Copyright 2016-2018 www.jeesuite.com.
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
package com.jeesuite.mybatis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.mybatis.datasource.DataSourceConfig;
import com.jeesuite.mybatis.datasource.DataSoureConfigHolder;
import com.jeesuite.mybatis.datasource.DatabaseType;
import com.jeesuite.mybatis.plugin.cache.CacheHandler;
import com.jeesuite.mybatis.plugin.rwseparate.RwRouteHandler;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class MybatisConfigs {
	
	public static final String CRUD_DRIVER = "jeesuite.mybatis.crudDriver";
	public static final String DB_TYPE = "jeesuite.mybatis.dbType";
	public static final String CACHE_ENABLED = "jeesuite.mybatis.cache.enabled";
	public static final String CACHE_EXPIRE_SECONDS = "jeesuite.mybatis.cache.expireSeconds";
	public static final String TENANT_SHARDDING_FIELD = "jeesuite.mybatis.tenant.sharddingField";
	public static final String INTERCEPTOR_HANDLERCLASS = "jeesuite.mybatis.interceptorHandlerClass";
	public static final String SOFT_DELETE_COLUMN_NAME = "jeesuite.mybatis.softDelete.columnName";
	public static final String SOFT_DELETE_FALSE_VALUE = "jeesuite.mybatis.softDelete.falseValue";
	public static final String OWNER_COLUMN_NAME = "jeesuite.mybatis.createBy.columnName";
	public static final String DEPT_COLUMN_NAME = "jeesuite.mybatis.department.columnName";
	public static final String ORG_BASE_PERM_KEY = "jeesuite.mybatis.currentOrgPermKey";
	
	private static Map<String, Properties> groupProperties = new HashMap<>();
	
	
	public static String getProperty(String group,String key,String defaultValue){
		if(!groupProperties.containsKey(group)) {
			synchronized (groupProperties) {
				String prefix = DataSourceConfig.DEFAULT_GROUP_NAME.equals(group) ? "jeesuite.mybatis" : group + ".jeesuite.mybatis";
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
	
	public static String getCrudDriver(){
		return ResourceUtils.getProperty(CRUD_DRIVER);
	}
	
	public static String getDbType(String group){
		return getProperty(group,DB_TYPE, DatabaseType.mysql.name()).toLowerCase();
	}
	
	public static boolean isCacheEnabled(String group) {
		return getBoolean(group,CACHE_ENABLED, false);
	}
	
	public static String getTenantSharddingField(String group) {
		return getProperty(group,TENANT_SHARDDING_FIELD,null);
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
	
	public static boolean isFieldSharddingTenant(String group) {
		return getTenantSharddingField(group) != null;
	}
	
	public static boolean isSchameSharddingTenant(String group) {
		return DataSoureConfigHolder.containsTenantConfig(group);
	}
	
	public static String[] getHandlerNames(String group){
        List<String> hanlders = new ArrayList<>();
        String customHandlers = getProperty(group, INTERCEPTOR_HANDLERCLASS, null);
		if(!StringUtils.isEmpty(customHandlers)){
			String[] customHanlderClass = StringUtils.tokenizeToStringArray(customHandlers, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
			hanlders.addAll(Arrays.asList(customHanlderClass));
		}

		if (isCacheEnabled(group)) {
			hanlders.add(CacheHandler.NAME);
		}
        //
		if (DataSoureConfigHolder.containsSlaveConfig()) {
			hanlders.add(RwRouteHandler.NAME);
		}
		
		return hanlders.toArray(new String[0]);
	}
}
