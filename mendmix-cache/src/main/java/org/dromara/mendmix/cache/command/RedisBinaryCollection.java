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
package org.dromara.mendmix.cache.command;

import java.util.ArrayList;
import java.util.List;

/**
 * 集合操作基类
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public abstract class RedisBinaryCollection extends RedisBase {

	protected long expireTime;//过期时间（秒）

	public RedisBinaryCollection(String key) {
		this(key,RedisBase.getDefaultExpireSeconds());
	}
	
	/**
	 * 指定组名
	 * @param key
	 * @param groupName
	 */
	public RedisBinaryCollection(String key,String groupName) {
		this(key,groupName,RedisBase.getDefaultExpireSeconds());
	}
	
	public RedisBinaryCollection(String key,long expireTime) {
		super(key);
		this.expireTime = expireTime;
	}
	
	public RedisBinaryCollection(String key,String groupName,long expireTime) {
		super(key,groupName,true);
		this.expireTime = expireTime;
	}
	
	protected <T> List<T> toObjectList(List<byte[]> datas) {
		List<T> result = new ArrayList<>();
    	if(datas == null)return result;
    	for (byte[] data : datas) {
			result.add((T)valueDerialize(data));
		}
		return result;
	}
	

}
