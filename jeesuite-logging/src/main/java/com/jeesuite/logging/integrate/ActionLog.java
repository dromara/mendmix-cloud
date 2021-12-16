package com.jeesuite.logging.integrate;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.jeesuite.common.util.DateUtils;

/**
 * 操作日志
 * <br>
 * Class Name   : ActionLog
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年8月22日
 */
@JsonInclude(Include.NON_NULL)
public class ActionLog implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private String appId;
	private String env;
	private String tenantId;
	private String clientType;
	private String actionName;
	private String actionKey;
	private String userId;
	private String userName;
	private String originAppId;
	private String requestIp;
	@JsonFormat(pattern=DateUtils.TIMESTAMP_PATTERN,timezone = "GMT+8")
	private Date requestAt;
	private int responseCode;
	private Map<String, String> requestParameters;
	private Object requestData;
	private Object responseData;
	private Integer useTime;
	private String requestId;
	private String exceptions;
	
	public String getAppId() {
		return appId;
	}
	public void setAppId(String appId) {
		this.appId = appId;
	}
	/**
	 * @return the env
	 */
	public String getEnv() {
		return env;
	}
	/**
	 * @param env the env to set
	 */
	public void setEnv(String env) {
		this.env = env;
	}

	/**
	 * @return the actionName
	 */
	public String getActionName() {
		return actionName;
	}
	/**
	 * @param actionName the actionName to set
	 */
	public void setActionName(String actionName) {
		this.actionName = actionName;
	}
	
	public String getTenantId() {
		return tenantId;
	}
	
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	
	public String getClientType() {
		return clientType;
	}
	
	public void setClientType(String clientType) {
		this.clientType = clientType;
	}
	
	public String getActionKey() {
		return actionKey;
	}
	
	public void setActionKey(String actionKey) {
		this.actionKey = actionKey;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public void setUserName(String userName) {
		this.userName = userName;
	}
	/**
	 * @return the originAppId
	 */
	public String getOriginAppId() {
		return originAppId;
	}
	/**
	 * @param originAppId the originAppId to set
	 */
	public void setOriginAppId(String originAppId) {
		this.originAppId = originAppId;
	}
	/**
	 * @return the requestIp
	 */
	public String getRequestIp() {
		return requestIp;
	}
	/**
	 * @param requestIp the requestIp to set
	 */
	public void setRequestIp(String requestIp) {
		this.requestIp = requestIp;
	}
	/**
	 * @return the requestAt
	 */
	public Date getRequestAt() {
		return requestAt;
	}
	/**
	 * @param requestAt the requestAt to set
	 */
	public void setRequestAt(Date requestAt) {
		this.requestAt = requestAt;
	}
	/**
	 * @param responseAt the responseAt to set
	 */
	public void setResponseAt(Date responseAt) {
		this.useTime = (int) (responseAt.getTime() - requestAt.getTime());
	}
	/**
	 * @return the responseCode
	 */
	public int getResponseCode() {
		return responseCode;
	}
	/**
	 * @param responseCode the responseCode to set
	 */
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}
	
	public Map<String, String> getRequestParameters() {
		return requestParameters;
	}
	public void setRequestParameters(Map<String, String> requestParameters) {
		this.requestParameters = requestParameters;
	}
	public Object getRequestData() {
		return requestData;
	}
	public void setRequestData(Object requestData) {
		this.requestData = requestData;
	}
	public Object getResponseData() {
		return responseData;
	}
	public void setResponseData(Object responseData) {
		this.responseData = responseData;
	}
	/**
	 * @return the useTime
	 */
	public Integer getUseTime() {
		return useTime;
	}
	/**
	 * @param useTime the useTime to set
	 */
	public void setUseTime(Integer useTime) {
		this.useTime = useTime;
	}
	/**
	 * @return the requestId
	 */
	public String getRequestId() {
		return requestId;
	}
	/**
	 * @param requestId the requestId to set
	 */
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	public String getExceptions() {
		return exceptions;
	}
	public void setExceptions(String exceptions) {
		this.exceptions = exceptions;
	}
	
	

}
