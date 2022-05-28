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
package com.mendmix.cache.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.cache.redis.JedisProviderFactory;
import com.mendmix.common.util.SerializeUtils;

import redis.clients.jedis.util.SafeEncoder;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月9日
 */
public class RedisBatchCommand {

	protected static final Logger logger = LoggerFactory.getLogger(RedisBatchCommand.class);
	
	protected static final String RESP_OK = "OK";
	
	/**
	 * 指定组批量写入字符串
	 * @param groupName 缓存组
	 * @param keyValueMap 
	 * @return
	 */
	public static boolean setStringsWithGroup(String groupName,Map<String, Object> keyValueMap){
		if(keyValueMap == null || keyValueMap.isEmpty())return false;
		String[] keysValues = new String[keyValueMap.size() * 2];
		int index = 0;
		for (String key : keyValueMap.keySet()) {
			if(keyValueMap.get(key) == null)continue;
			keysValues[index++] = key;
			keysValues[index++] = keyValueMap.get(key).toString();
		}
		try {			
			if(JedisProviderFactory.isCluster(groupName)){
				return JedisProviderFactory.getMultiKeyJedisClusterCommands(groupName).mset(keysValues).equals(RESP_OK);
			}else{
				return JedisProviderFactory.getMultiKeyCommands(groupName).mset(keysValues).equals(RESP_OK);
			}
		} finally {
			JedisProviderFactory.getJedisProvider(groupName).release();
		}
	}
	
	/**
	 * 默认组批量写入字符串
	 * @param groupName 缓存组
	 * @param keyValueMap 
	 * @return
	 */
	public static boolean setStrings(Map<String, Object> keyValueMap){
		return setStringsWithGroup(null, keyValueMap);
	}
	
	/**
	 * 指定组批量写入对象
	 * @param groupName 缓存组
	 * @param keyValueMap 
	 * @return
	 */
	public static boolean setObjectsWithGroup(String groupName,Map<String, Object> keyValueMap){
		if(keyValueMap == null || keyValueMap.isEmpty())return false;
		byte[][] keysValues = new byte[keyValueMap.size() * 2][];
		int index = 0;
		for (String key : keyValueMap.keySet()) {
			if(keyValueMap.get(key) == null)continue;
			keysValues[index++] = SafeEncoder.encode(key);
			keysValues[index++] = SerializeUtils.serialize(keyValueMap.get(key));
		}
		
        try {			
        	if(JedisProviderFactory.isCluster(groupName)){
        		return JedisProviderFactory.getMultiKeyBinaryJedisClusterCommands(groupName).mset(keysValues).equals(RESP_OK);
        	}else{
        		return JedisProviderFactory.getMultiKeyBinaryCommands(groupName).mset(keysValues).equals(RESP_OK);
        	}
		} finally {
			JedisProviderFactory.getJedisProvider(groupName).release();
		}
	}
	
	/**
	 * 默认组批量写入对象
	 * @param groupName 缓存组
	 * @param keyValueMap 
	 * @return
	 */
	public static boolean setObjects(Map<String, Object> keyValueMap){
		return setObjectsWithGroup(null, keyValueMap);
	}
	
	/**
	 * 按key批量从redis获取值（指定缓存组名）
	 * @param groupName
	 * @param keys
	 * @return list<String>
	 */
	public static List<String> getStringsWithGroup(String groupName,String...keys){
        try {
        	if(JedisProviderFactory.isCluster(groupName)){
        		return JedisProviderFactory.getMultiKeyJedisClusterCommands(groupName).mget(keys);
        	}else{
        		return JedisProviderFactory.getMultiKeyCommands(groupName).mget(keys);
        	}
		} finally {
			JedisProviderFactory.getJedisProvider(groupName).release();
		}
	}

	public static List<String> getStrings(String...keys){
		return getStringsWithGroup(null, keys);
	}
	
	public static boolean removeStringsWithGroup(String groupName,String...keys){
        try {			
        	if(JedisProviderFactory.isCluster(groupName)){
        		return JedisProviderFactory.getMultiKeyJedisClusterCommands(groupName).del(keys) == 1;
        	}else{
        		return JedisProviderFactory.getMultiKeyCommands(groupName).del(keys) == 1;
        	}
		} finally {
			JedisProviderFactory.getJedisProvider(groupName).release();
		}
	}
	
    public static boolean removeStrings(String...keys){
    	return removeStringsWithGroup(null, keys);
	}
    
    
    public static boolean removeObjectsWithGroup(String groupName,String...keys){
    	byte[][] byteKeys = SafeEncoder.encodeMany(keys);
        try {			
        	if(JedisProviderFactory.isCluster(groupName)){
        		return JedisProviderFactory.getMultiKeyBinaryJedisClusterCommands(groupName).del(byteKeys) == 1;
        	}else{
        		return JedisProviderFactory.getMultiKeyBinaryCommands(groupName).del(byteKeys) == 1;
        	}
		} finally {
			JedisProviderFactory.getJedisProvider(groupName).release();
		}
	}
	
    public static boolean removeObjects(String...keys){
    	return removeObjectsWithGroup(null, keys);
	}
	
	public static <T> List<T> getObjectsWithGroup(String groupName, String... keys) {
		byte[][] byteKeys = SafeEncoder.encodeMany(keys);

		try {
			if (JedisProviderFactory.isCluster(groupName)) {
				List<byte[]> bytes = JedisProviderFactory.getMultiKeyBinaryJedisClusterCommands(groupName)
						.mget(byteKeys);
				return listDerialize(bytes);
			} else {
				List<byte[]> bytes = JedisProviderFactory.getMultiKeyBinaryCommands(groupName).mget(byteKeys);
				return listDerialize(bytes);
			}
		} finally {
			JedisProviderFactory.getJedisProvider(groupName).release();
		}
	}
	
	public static <T> List<T> getObjects(String...keys){
		return getObjectsWithGroup(null, keys);
	}
	
	public static void removeByKeyPrefix(String keyPrefix){
		removeByKeyPrefix(null, keyPrefix);
	}
	
	public static void removeByKeyPrefix(String group,String keyPrefix){
		try {			
			Set<String> keys = JedisProviderFactory.getMultiKeyCommands(group).keys(keyPrefix +"*");
			if(keys != null && keys.size() > 0){
				RedisBatchCommand.removeObjectsWithGroup(group,keys.toArray(new String[0]));
			}
		} finally {
			JedisProviderFactory.getJedisProvider(group).release();
		}
	}

	private static <T> T valueDerialize(byte[] bytes) {
		if(bytes == null)return null;
		try {
			return (T)SerializeUtils.deserialize(bytes);
		} catch (Exception e) {
			return null;
		}
	}
	
	private static <T> List<T> listDerialize(List<byte[]> datas){
		List<T> list = new ArrayList<>();
		if(datas == null)return list;
         for (byte[] bs : datas) {
        	 list.add((T)valueDerialize(bs));
		}
		return list;
	}
}
