/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface CacheAdapter {

    <T> T get(String key);
	
	String getStr(String key);
	
	void set(String key,Object value,long expireSeconds);
	
	void setStr(String key,String value,long expireSeconds);
	
	void remove(String...keys);
	
	boolean exists(String key);
	
	long size(String key);
	
	long getExpireIn(String key,TimeUnit timeUnit);
	
	void setExpire(String key,long expireSeconds);
	
	boolean setIfAbsent(String key, Object value,long timeout,TimeUnit timeUnit);

	void addStrItemToList(String key,String item);
	
	List<String> getStrListItems(String key,int start,int end);
	
	long getListSize(String key);
	
	void removeListValue(String key,String value);
	
	void setMapValue(String key,String field,Object value);
	
	void setMapValues(String key, Map<String,Object> map);
	
	<T> T getMapValue(String key, String field);
	
	<T> Map<String, T> getMapValues(String key);
	
	<T> Map<String, T> getMapValues(String key, Collection<String> fields);
	
    void setMapStringValue(String key,String field,String value);
	
	void setMapStringValues(String key, Map<String,String> map);
	
	String getMapStringValue(String key, String field);
	
	Map<String, String> getMapStringValues(String key);
	
	Map<String, String> getMapStringValues(String key, Collection<String> fields);
	
	void removeMapValue(String key, String field);
	
	boolean hasMapValue(String key, String field);
	
	Set<String> getKeys(String pattern);

}
