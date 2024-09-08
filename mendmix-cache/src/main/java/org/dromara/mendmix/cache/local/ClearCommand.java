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
package org.dromara.mendmix.cache.local;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;

import org.dromara.mendmix.common.util.JsonUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月4日
 */
public class ClearCommand implements Serializable {

	private static final long serialVersionUID = 1137020215396485376L;

	public final static byte DELETE_KEY = 0x01; // 删除缓存
	public final static byte CLEAR = 0x02; // 清除缓存

	private String cacheName;
	private String key;
	private String origin;

	private static String CURRENT_NODE_ID;

	static {
		try {
			CURRENT_NODE_ID = InetAddress.getLocalHost().getHostName() + "_"
					+ RandomStringUtils.random(6, true, true).toLowerCase();
		} catch (Exception e) {
			CURRENT_NODE_ID = UUID.randomUUID().toString();
		}
	}

	public String getCacheName() {
		return cacheName;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public ClearCommand() {
	}

	public ClearCommand(String cacheName, String key) {
		super();
		this.origin = CURRENT_NODE_ID;
		this.cacheName = cacheName;
		this.key = key;
	}

	public String serialize() {
		return JsonUtils.toJson(this);
	}
	
	public boolean isLocalCommand(){
		return CURRENT_NODE_ID.equals(origin);
	}

	public static ClearCommand deserialize(String json) {
		return JsonUtils.toObject(json, ClearCommand.class);
	}
}
