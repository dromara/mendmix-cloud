package com.jeesuite.common.model;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.jeesuite.common.util.SimpleCryptUtils;


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
	private String userType;//用户类型类型
	private List<String> tenantScopes;

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

	public String getUserType() {
		return userType;
	}
	public void setUserType(String userType) {
		this.userType = userType;
	}
	
	
	public List<String> getTenantScopes() {
		return tenantScopes;
	}
	
	public void setTenantScopes(List<String> tenantScopes) {
		this.tenantScopes = tenantScopes;
	}
	
	public String toEncodeString(){
		StringBuilder builder = new StringBuilder();
		builder.append(id).append(CONTACT_CHAR);
		builder.append(trimToPlaceHolder(name)).append(CONTACT_CHAR);
		builder.append(trimToPlaceHolder(userType)).append(CONTACT_CHAR);
		return SimpleCryptUtils.encrypt(builder.toString());
	}
	
	public static AuthUser decode(String encodeString){
		if(StringUtils.isBlank(encodeString))return null;
		encodeString = SimpleCryptUtils.decrypt(encodeString);
		String[] splits = encodeString.split(CONTACT_CHAR);
		AuthUser user = new AuthUser();
		user.setId(placeHolderToNull(splits[0]));
		user.setName(placeHolderToNull(splits[1]));
		user.setUserType(placeHolderToNull(splits[2]));
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
