/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.security.model;

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
	private String httpMethod;
	private String grantType;
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getHttpMethod() {
		return httpMethod;
	}
	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}
	public String getGrantType() {
		return grantType;
	}
	public void setGrantType(String grantType) {
		this.grantType = grantType;
	}
	
	

}
