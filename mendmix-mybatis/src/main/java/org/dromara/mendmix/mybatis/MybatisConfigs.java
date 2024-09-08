/*
 * Copyright 2016-2018 dromara.org.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.mybatis.datasource.DataSourceConfig;
import org.dromara.mendmix.mybatis.datasource.DataSoureConfigHolder;
import org.dromara.mendmix.mybatis.datasource.DatabaseType;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class MybatisConfigs {
	
	private static Map<String, Properties> groupProperties = new HashMap<>();
	
	private static final String DEFAULT_CONFIG_PREFIX = "mendmix-cloud.";
	public static final String DB_TYPE = "mendmix-cloud.mybatis.dbType";
	public static final String CACHE_ENABLED = "mendmix-cloud.mybatis.cache.enabled";
	public static final String DATA_PERM_ENABLED = "mendmix-cloud.mybatis.dataPermission.enabled";
	public static final String CACHE_EXPIRE_SECONDS = "mendmix-cloud.mybatis.cache.expireSeconds";
	public static final String TENANT_COLUMN_NAME = "mendmix-cloud.mybatis.tenant.columnName";
	public static final String BUNIT_COLUMN_NAME = "mendmix-cloud.mybatis.bUnit.columnName";
	public static final String TENANT_IGNORE_USER_TYPE = "mendmix-cloud.mybatis.tenant.ignoreUserType";
	public static final String INTERCEPTOR_HANDLERCLASS = "mendmix-cloud.mybatis.interceptorHandlerClass";
	public static final String SOFT_DELETE_COLUMN_NAME = "mendmix-cloud.mybatis.softDelete.columnName";
	public static final String SOFT_DELETE_FALSE_VALUE = "mendmix-cloud.mybatis.softDelete.falseValue";
	public static final String CREATED_BY_COLUMN_NAME = "mendmix-cloud.mybatis.createdBy.columnName";
	public static final String DEPT_COLUMN_NAME = "mendmix-cloud.mybatis.department.columnName";
	
	private static final List<String> ignoreTenantMappperPackages = ResourceUtils.getList("mendmix-cloud.mybatis.ignoreTenantMappperPackages");
	
	public static final String ORG_DATA_PERM_NAME = ResourceUtils.getProperty("mendmix-cloud.mybatis.dataPermission.orgPermName", "orgPermName");
	public static final boolean DATA_PERM_ALL_MATCH_MODE_ENABLED = ResourceUtils.getBoolean("mendmix-cloud.mybatis.dataPermission.allMatchMode.enabled",true);
	public static final boolean DATA_PERM_HANDLE_OWNER = ResourceUtils.getBoolean("mendmix-cloud.mybatis.dataPermission.defaultHandleOwner", false);
	public static final boolean DATA_PERM_ORG_USING_FULL_CODE_MODE = ResourceUtils.getBoolean("mendmix-cloud.mybatis.dataPermission.orgUsingFullCodeMode", false);
	public static final boolean DATA_PERM_USING_GROUP_MODE = ResourceUtils.getBoolean("mendmix-cloud.mybatis.dataPermission.groupMode",false);
	public static final boolean DATA_PERM_GROUP_MISSING_MATCH_NONE = ResourceUtils.getBoolean("mendmix-cloud.mybatis.dataPermission.groupMissingMatchNone",true);
	public static final boolean DATA_PERM_ORG_ID_USING_CODE = ResourceUtils.getBoolean("mendmix-cloud.mybatis.dataPermission.orgIdUsingCode",false);
	public static final boolean DATA_PERM_MULTI_SCOPE_MODE = ResourceUtils.getBoolean("mendmix-cloud.mybatis.dataPermission.multiScopeMode");
	public static final boolean DATA_PERM_STRICT_MODE = ResourceUtils.getBoolean("mendmix-cloud.mybatis.dataPermission.strictMode");
	public static final boolean DATA_PERM_INNER_JOIN_USING_ON = ResourceUtils.getBoolean("mendmix-cloud.mybatis.dataPermission.innerJoinUsingOn",true);
	
	public static final int DB_OFFSET = ResourceUtils.getInt("mendmix-cloud.using-db-time.offset", 0);
	
	public static final int QUERY_MAX_LIMIT = ResourceUtils.getInt("mendmix-cloud.mybatis.query.maxLimit", 10000);
	public static final int UPDATE_MAX_LIMIT = ResourceUtils.getInt("mendmix-cloud.mybatis.update.maxLimit", 5000);

	public static String getProperty(String group,String key,String defaultValue){
		if(!groupProperties.containsKey(group)) {
			synchronized (groupProperties) {
				String prefix = DataSourceConfig.DEFAULT_GROUP_NAME.equals(group) ? "mendmix-cloud.mybatis" : group + ".mybatis";
				MybatisConfigs.addProperties(group, ResourceUtils.getAllProperties(prefix));
			}
		}
		
		String value = groupProperties.get(group).getProperty(key);
		String fixedKey = null;
		if(value == null) {
			if(DataSourceConfig.DEFAULT_GROUP_NAME.equals(group) && !key.startsWith(DEFAULT_CONFIG_PREFIX)) {
				fixedKey = DEFAULT_CONFIG_PREFIX + key;
			}else if(key.startsWith(DEFAULT_CONFIG_PREFIX)){
				fixedKey = key.substring(DEFAULT_CONFIG_PREFIX.length());
			}
			if(fixedKey != null) {
				value = groupProperties.get(group).getProperty(fixedKey);
			}
		}
		if(value == null) {
			value = ResourceUtils.getAnyProperty(key,fixedKey);
		}
		
		return StringUtils.defaultString(value, defaultValue);
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
	public static boolean isDataPermissionEnabled(String group) {
		return getBoolean(group,DATA_PERM_ENABLED, false);
	}
	
	public static String getTenantColumnName(String group) {
		return getProperty(group,TENANT_COLUMN_NAME,null);
	}
	
	public static boolean isFieldSharddingTenant(String group) {
		return getTenantColumnName(group) != null;
	}
	
	public static boolean isSchameSharddingTenant(String group) {
		return DataSoureConfigHolder.containsTenantConfig(group);
	}
	
	public static String getSoftDeleteColumn(String group) {
		return getProperty(group,SOFT_DELETE_COLUMN_NAME,null);
	}
	
	public static String getSoftDeletedFalseValue(String group) {
		return getProperty(group,SOFT_DELETE_FALSE_VALUE,"0");
	}
	
	public static String getCreatedByColumnName(String group) {
		return getProperty(group,CREATED_BY_COLUMN_NAME,"created_by");
	}
	
	public static String getDeptColumnName(String group) {
		return getProperty(group,DEPT_COLUMN_NAME,null);
	}
	
	public static boolean ignoreTenant(String mappper) {
		if(ignoreTenantMappperPackages.isEmpty()) {
			return false;
		}
		return ignoreTenantMappperPackages.stream().anyMatch(o -> mappper.contains(o));
	}
	
	public static String getBusinessUnitColumn(String group){
		return getProperty(group, BUNIT_COLUMN_NAME, null);
	}
	
	public static List<String> getCustomHandlerNames(String group){
        List<String> hanlders = new ArrayList<>();
        String customHandlers = getProperty(group, INTERCEPTOR_HANDLERCLASS, null);
		if(!StringUtils.isEmpty(customHandlers)){
			String[] customHanlderClass = org.springframework.util.StringUtils.tokenizeToStringArray(customHandlers, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
			hanlders.addAll(Arrays.asList(customHanlderClass));
		}
		return hanlders;
	}
}
