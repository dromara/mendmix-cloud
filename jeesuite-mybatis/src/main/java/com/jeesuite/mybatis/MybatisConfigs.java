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
import com.jeesuite.mybatis.plugin.cache.CacheHandler;
import com.jeesuite.mybatis.plugin.dataprofile.DataProfileHandler;
import com.jeesuite.mybatis.plugin.pagination.PaginationHandler;
import com.jeesuite.mybatis.plugin.rwseparate.RwRouteHandler;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class MybatisConfigs {
	
	public static final String CRUD_DRIVER = "jeesuite.mybatis.crudDriver";
	public static final String DB_TYPE = "jeesuite.mybatis.dbType";
	public static final String CACHE_ENABLED = "jeesuite.mybatis.cacheEnabled";
	public static final String CACHE_NULL_VALUE = "jeesuite.mybatis.nullValueCache";
	public static final String CACHE_EXPIRE_SECONDS = "jeesuite.mybatis.cacheExpireSeconds";
	public static final String CACHE_DYNAMIC_EXPIRE = "jeesuite.mybatis.dynamicExpire";
	public static final String RW_ROUTE_ENABLED = "jeesuite.mybatis.rwRouteEnabled";
	public static final String PAGINATION_ENABLED = "jeesuite.mybatis.paginationEnabled";
	public static final String TENANT_MODE_ENABLED = "jeesuite.mybatis.tenantModeEnabled";
	public static final String DATA_PROFILE_ENABLED = "jeesuite.mybatis.dataProfileEnabled";
	public static final String PAGINATION_MAX_LIMIT = "jeesuite.mybatis.pagination.maxLimit";
	public static final String INTERCEPTOR_HANDLERCLASS = "jeesuite.mybatis.interceptorHandlerClass";
	
	private static Map<String, Properties> groupProperties = new HashMap<>();
	
	public static void addProperties(String group,Properties properties){
		groupProperties.put(group, properties);
	}
	
	public static String getProperty(String group,String key,String defaultValue){
		if(!groupProperties.containsKey(group))return defaultValue;
		if(!"default".equals(group)){			
			key = group + "." + key;
		}
		return groupProperties.get(group).getProperty(key, defaultValue);
	}
	
	public static boolean getBoolean(String group,String key,boolean defaultValue){
		return Boolean.parseBoolean(getProperty(group, key, String.valueOf(defaultValue)));
	}
	
	public static String getCrudDriver(String group){
		return getProperty(group,CRUD_DRIVER, "default");
	}
	
	public static String getDbType(String group){
		return getProperty(group,DB_TYPE, "Mysql");
	}
	
	public static boolean isCacheEnabled(String group) {
		return getBoolean(group,CACHE_ENABLED, false);
	}
	
	public static boolean isRwRouteEnabled(String group) {
		return getBoolean(group,RW_ROUTE_ENABLED, false);
	}
	
	public static boolean isPaginationEnabled(String group) {
		return getBoolean(group,PAGINATION_ENABLED, true);
	}
	
	public static boolean isTenantModeEnabled() {
		return ResourceUtils.getBoolean(TENANT_MODE_ENABLED, false);
	}
	
	public static boolean isDataProfileEnabled() {
		return ResourceUtils.getBoolean(DATA_PROFILE_ENABLED, false);
	}
	
	public static int getPaginationMaxLimit(){
		return ResourceUtils.getInt(PAGINATION_MAX_LIMIT, 0);
	}
	
	public static String[] getHanlderNames(String group){
        List<String> hanlders = new ArrayList<>();
		if(ResourceUtils.containsProperty(INTERCEPTOR_HANDLERCLASS)){
			String[] customHanlderClass = StringUtils.tokenizeToStringArray(ResourceUtils.getProperty(INTERCEPTOR_HANDLERCLASS), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
			hanlders.addAll(Arrays.asList(customHanlderClass));
		}
		
		if (isCacheEnabled(group)) {
			hanlders.add(CacheHandler.NAME);
		}

		if (isRwRouteEnabled(group)) {
			hanlders.add(RwRouteHandler.NAME);
		}
		
		if (isPaginationEnabled(group)) {
			hanlders.add(PaginationHandler.NAME);
		}
		
		if (isDataProfileEnabled()) {
			hanlders.add(DataProfileHandler.NAME);
		}
		
		return hanlders.toArray(new String[0]);
	}
}
