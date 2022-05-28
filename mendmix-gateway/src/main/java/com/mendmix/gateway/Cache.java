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
package com.mendmix.gateway;

/**
 * 
 * <br>
 * Class Name   : Cache
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年7月10日
 */
public interface Cache {

	void setString(String key,String value);
	String getString(String key);
	void setObject(String key,Object value);
	<T> T getObject(String key);
	void remove(String key);
	void removeAll();
	boolean exists(String key);
	void setMapValue(String key,String field,Object value);
	<T> T getMapValue(String key,String field);
	void updateExpireTime(String key);
	
}
