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
package com.mendmix.common.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.mendmix.common.util.SimpleCryptUtils;


/**
 * 
 * <br>
 * Class Name   : AuthUser
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年10月17日
 */
public class AuthUser {

	private static final String CONTACT_CHAR = "#";
	private static final String PLACEHOLDER_CHAR = "{-}";
	
	private String id;
	private String name;
	private String type;//用户类型类型
	private String deptId;
	private String postId;
	private String principalType;
	private String principalId;
	private boolean admin;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getDeptId() {
		return deptId;
	}
	public void setDeptId(String deptId) {
		this.deptId = deptId;
	}
	
	public String getPostId() {
		return postId;
	}
	public void setPostId(String postId) {
		this.postId = postId;
	}
	public String getPrincipalType() {
		return principalType;
	}
	
	public void setPrincipalType(String principalType) {
		this.principalType = principalType;
	}
	
	public String getPrincipalId() {
		return principalId;
	}
	public void setPrincipalId(String principalId) {
		this.principalId = principalId;
	}
	public boolean isAdmin() {
		return admin;
	}
	public void setAdmin(boolean admin) {
		this.admin = admin;
	}
	
	public String toEncodeString(){
		StringBuilder builder = new StringBuilder();
		builder.append(id).append(CONTACT_CHAR);
		builder.append(trimToPlaceHolder(name)).append(CONTACT_CHAR);
		builder.append(trimToPlaceHolder(type)).append(CONTACT_CHAR);
		builder.append(trimToPlaceHolder(deptId)).append(CONTACT_CHAR);
		builder.append(trimToPlaceHolder(postId)).append(CONTACT_CHAR);
		builder.append(trimToPlaceHolder(principalType)).append(CONTACT_CHAR);
		builder.append(trimToPlaceHolder(principalId)).append(CONTACT_CHAR);
		builder.append(admin).append(CONTACT_CHAR);
		return SimpleCryptUtils.encrypt(builder.toString());
	}
	
	public static AuthUser decode(String encodeString){
		
		if(StringUtils.isBlank(encodeString))return null;
		encodeString = SimpleCryptUtils.decrypt(encodeString);
		String[] splits = encodeString.split(CONTACT_CHAR);
		AuthUser user = new AuthUser();
		user.setId(placeHolderToNull(splits[0]));
		user.setName(placeHolderToNull(splits[1]));
		user.setType(placeHolderToNull(splits[2]));
		user.setDeptId(placeHolderToNull(splits[3]));
		user.setPostId(placeHolderToNull(splits[4]));
		user.setPrincipalType(placeHolderToNull(splits[5]));
		user.setPrincipalId(placeHolderToNull(splits[6]));
		user.setAdmin(Boolean.parseBoolean(splits[7]));
		
		return user;
	}
	

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}
	
	private static String trimToPlaceHolder(String value){
		value = StringUtils.trimToNull(value);
		return value == null ? PLACEHOLDER_CHAR : value;
	}
	
	private static String placeHolderToNull(String value){
		return PLACEHOLDER_CHAR.equals(value) ? null : value;
	}
}
