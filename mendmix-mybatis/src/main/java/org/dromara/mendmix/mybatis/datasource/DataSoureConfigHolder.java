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
package org.dromara.mendmix.mybatis.datasource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.dromara.mendmix.common.util.ResourceUtils;

/**
 * 
 * 
 * <br>
 * Class Name   : DataSoureConfigHolder
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Sep 11, 2021
 */
public class DataSoureConfigHolder {

	private static Map<String, List<DataSourceConfig>> allGroupConfigs;
	
	private static Map<String, List<DataSourceConfig>> getAllGroupConfigs() {
		if(allGroupConfigs == null) {
			Properties properties = ResourceUtils.getAllProperties(".*(\\.db\\.).*", false);
			allGroupConfigs = resolveConfigs(properties);
		}
		return allGroupConfigs;
	}

	public static List<String> getGroups(){
		return new ArrayList<>(getAllGroupConfigs().keySet());
	}
	
	public static List<DataSourceConfig> getConfigs(String group){
		return getAllGroupConfigs().get(group);
	}
	
	public static boolean containsSlaveConfig(){
		for (String group : getAllGroupConfigs().keySet()) {
			if(allGroupConfigs.get(group).stream().anyMatch(o -> !o.isMaster())) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean containsTenantConfig(String group){
		if(getAllGroupConfigs().get(group).stream().anyMatch(o -> StringUtils.isNotBlank(o.getTenantRouteKey()))) {
			return true;
		}
		return false;
	}
	
	public synchronized static Map<String, List<DataSourceConfig>> resolveConfigs(Properties properties){
		
		Map<String, List<DataSourceConfig>> groupConfigs = new HashMap<>();
		
		Field[] fields = FieldUtils.getAllFields(DataSourceConfig.class);
		for (Field field : fields) {
			field.setAccessible(true);
		}
		
		Map<String,DataSourceConfig> configs = new HashMap<>();
		
		Set<Entry<Object, Object>> entrySet = properties.entrySet();
		
		DataSourceConfig config;
		String dsKey;
		for (Entry<Object, Object> entry : entrySet) {
			String key = entry.getKey().toString();
			if(!key.contains("master") && !key.contains("slave")) {
				continue;
			}
			
			config = buildDataSourceConfig(key,fields);
			dsKey = config.dataSourceKey();
			
			if(configs.containsKey(dsKey)) {
				config = configs.get(dsKey);
			}else {
				configs.put(dsKey, config);
			}
		}
		
		List<DataSourceConfig> groupList;
		for (DataSourceConfig dsConfig : configs.values()) {
			groupList = groupConfigs.get(dsConfig.getGroup());
			if(groupList == null) {
				groupConfigs.put(dsConfig.getGroup(), (groupList = new ArrayList<>()));
			}
			groupList.add(dsConfig);
		}
	    
	    return groupConfigs;
	}
	
	//group[default].tenant[abc].master.db.url=xx
	//group[default].tenant[abc].slave[0].db.url=xx
	private static DataSourceConfig buildDataSourceConfig(String propKey,Field[] fields) {
		String[] arrays = StringUtils.split(propKey, ".");
		DataSourceConfig config = new DataSourceConfig();
		for (String item : arrays) {
			if(item.startsWith("group")) {
				config.setGroup(parseFieldValue(item));
			}else if(item.startsWith("tenant")) {
				config.setTenantRouteKey(parseFieldValue(item));
			}else if(item.startsWith("slave")) {
				String value = parseFieldValue(item);
				if(value != null) {
					config.setIndex(Integer.parseInt(value));
				}
			}else if(item.equals("master")) {
				config.setMaster(true);
			}
		}
		//
		String propKeyPrefix = propKey.substring(0,propKey.lastIndexOf(".") + 1);
		for (Field field : fields) {
			String propValue = ResourceUtils.getProperty(propKeyPrefix + field.getName());
			if(StringUtils.isBlank(propValue))continue;
			setFieldValue(config, field, propValue);
		}
		
		return config;
	}
	
	private static String parseFieldValue(String field) {
		if(!field.contains("["))return null;
		return StringUtils.split(field, "[]")[1];
	}
	
	private static void setFieldValue(Object object,Field field,String value) {
		Object _value = value;
		if(field.getType() == int.class || field.getType() == Integer.class){
			_value = Integer.parseInt(value);
		}else if(field.getType() == long.class || field.getType() == Long.class){
			_value = Long.parseLong(value);
		}else if(field.getType() == boolean.class || field.getType() == Boolean.class){
			_value = Boolean.parseBoolean(value);
		}
		try {
			field.set(object, _value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
