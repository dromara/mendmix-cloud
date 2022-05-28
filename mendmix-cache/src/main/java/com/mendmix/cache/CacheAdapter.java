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
package com.mendmix.cache;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface CacheAdapter {

	<T> T get(String key);
	
	String getString(String key);
	
	void set(String key,Object value,long expireSeconds);
	
	void setString(String key,Object value,long expireSeconds);
	
	void remove(String...keys);
	
	boolean exists(String key);

	void addListItems(String key,String ...items);
	
	List<String> getListItems(String key,int start,int end);
	
	long getListSize(String key);
	
	boolean setIfAbsent(String key,String value,long expireSeconds);
	
	void setMapItem(String key,String field,String value);
	
	Map<String, String> getMap(String key);
	
	String getMapItem(String key,String field);
	
	void setExpire(String key,long expireSeconds);
	
	void setExpireAt(String key,Date expireAt);
	
	long getTtl(String key);
}
