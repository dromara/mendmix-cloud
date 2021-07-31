package com.jeesuite.springweb.logging;

import java.io.Serializable;
import java.util.Date;

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
	private String appName;
	private String actionName;
	private String actionUri;
	private String actionUserId;
	private String actionUserName;
	private String originAppId;
	private String requestIp;
	@JsonFormat(pattern=DateUtils.TIMESTAMP_PATTERN,timezone = "GMT+8")
	private Date requestAt;
	private int responseCode;
	private String requestData;
	private String responseData;
	private Integer useTime;
	private String requestId;
	
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
	 * @return the appName
	 */
	public String getAppName() {
		return appName;
	}
	/**
	 * @param appName the appName to set
	 */
	public void setAppName(String appName) {
		this.appName = appName;
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
	/**
	 * @return the actionUri
	 */
	public String getActionUri() {
		return actionUri;
	}
	/**
	 * @param actionUri the actionUri to set
	 */
	public void setActionUri(String actionUri) {
		this.actionUri = actionUri;
	}
	/**
	 * @return the actionUserId
	 */
	public String getActionUserId() {
		return actionUserId;
	}
	/**
	 * @param actionUserId the actionUserId to set
	 */
	public void setActionUserId(String actionUserId) {
		this.actionUserId = actionUserId;
	}
	/**
	 * @return the actionUserName
	 */
	public String getActionUserName() {
		return actionUserName;
	}
	/**
	 * @param actionUserName the actionUserName to set
	 */
	public void setActionUserName(String actionUserName) {
		this.actionUserName = actionUserName;
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
	/**
	 * @return the requestData
	 */
	public String getRequestData() {
		return requestData;
	}
	/**
	 * @param requestData the requestData to set
	 */
	public void setRequestData(String requestData) {
		this.requestData = requestData;
	}
	/**
	 * @return the responseData
	 */
	public String getResponseData() {
		return responseData;
	}
	/**
	 * @param responseData the responseData to set
	 */
	public void setResponseData(String responseData) {
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
	

}
