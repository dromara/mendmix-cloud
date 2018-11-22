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

import java.util.Properties;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class Configs {
	private static Properties properties = new Properties();
	
	public static final String CRUD_DRIVER = "db.crud.driver";
	public static final String DB_TYPE = "db.type";
	public static final String CACHE_NULL_VALUE = "cache.nullValue";
	public static final String CACHE_EXPIRE_SECONDS = "cache.expire.seconds";
	public static final String CACHE_DYNAMIC_EXPIRE = "cache.dynamic.expire";
	
	public static void addPropertis(Properties properties){
		Configs.properties.putAll(properties);
	}
	
	public static void addProperty(String key,String value){
		properties.setProperty(key, value);
	}
	
	public static String getProperty(String key,String defaultValue){
		return properties.getProperty(key, defaultValue);
	}
	
	public static String getCrudDriver(){
		return getProperty(CRUD_DRIVER, "default");
	}
	
	public static String getDbType(){
		return getProperty(DB_TYPE, "Mysql");
	}
}
