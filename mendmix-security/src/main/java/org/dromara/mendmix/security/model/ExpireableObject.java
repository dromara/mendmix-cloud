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
package org.dromara.mendmix.security.model;

/**
 * 
 * 
 * <br>
 * Class Name   : ExpireableObject
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date May 1, 2021
 */
public class ExpireableObject {

	private Object target;
	private long expireAt;
	
	
	public ExpireableObject() {}
	
	public ExpireableObject(Object target, long expireAt) {
		super();
		this.target = target;
		this.expireAt = expireAt;
	}

	public Object getTarget() {
		return target;
	}
	public void setTarget(Object target) {
		this.target = target;
	}
	public long getExpireAt() {
		return expireAt;
	}
	public void setExpireAt(long expireAt) {
		this.expireAt = expireAt;
	}
	
	
}
