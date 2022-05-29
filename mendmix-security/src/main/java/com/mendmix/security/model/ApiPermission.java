/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.security.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.mendmix.common.constants.PermissionLevel;

/**
 * 
 * <br>
 * Class Name   : ApiPermissionInfo
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年3月6日
 */
public class ApiPermission {

	private String uri;
	private String method;
	private PermissionLevel permissionLevel;
	private String permissionKey;

	public ApiPermission() {}
	
	
	
	public ApiPermission(String httpMethod,String uri, PermissionLevel permissionLevel) {
		this.method = httpMethod;
		this.uri = uri;
		this.permissionLevel = permissionLevel;
	}

	public String getUri() {
		return uri;
	}
	
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public String getMethod() {
		return method;
	}
	
	public void setMethod(String method) {
		this.method = method;
	}


	public PermissionLevel getPermissionLevel() {
		return permissionLevel;
	}


	public void setPermissionLevel(PermissionLevel permissionLevel) {
		this.permissionLevel = permissionLevel;
	}
	
	public void setPermissionLevel(String permissionLevel) {
		this.permissionLevel = PermissionLevel.valueOf(permissionLevel);
	}

	public String getPermissionKey() {
		return permissionKey;
	}

	public void setPermissionKey(String permissionKey) {
		this.permissionKey = permissionKey;
	}



	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}
	
	
	
}
