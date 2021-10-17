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

	private String appId;
	private String routeName;
	private String uri;
	private String httpMethod;
	private String grantType;
	/**
	 * @return the appId
	 */
	public String getAppId() {
		return appId;
	}
	/**
	 * @param appId the appId to set
	 */
	public void setAppId(String appId) {
		this.appId = appId;
	}
	/**
	 * @return the routeName
	 */
	public String getRouteName() {
		return routeName;
	}
	/**
	 * @param routeName the routeName to set
	 */
	public void setRouteName(String routeName) {
		this.routeName = routeName;
	}
	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}
	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}
	/**
	 * @return the httpMethod
	 */
	public String getHttpMethod() {
		return httpMethod;
	}
	/**
	 * @param httpMethod the httpMethod to set
	 */
	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}
	/**
	 * @return the grantType
	 */
	public String getGrantType() {
		return grantType;
	}
	/**
	 * @param grantType the grantType to set
	 */
	public void setGrantType(String grantType) {
		this.grantType = grantType;
	}
	
	

}
