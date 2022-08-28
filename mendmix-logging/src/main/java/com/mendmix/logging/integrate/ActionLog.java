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
package com.mendmix.logging.integrate;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.annotation.ApiMetadata;
import com.mendmix.common.model.ApiInfo;
import com.mendmix.common.model.AuthUser;
import com.mendmix.logging.helper.LogMessageFormat;

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
	
	public static final String IGNORE_FLAG = "[ignore]";
	
	private String appId;
	private String env;
	private String tenantId;
	private String clientType;
	private String actionName;
	private String actionKey;
	private String userId;
	private String userName;
	private String moduleId;
	private String requestIp;
	private Date requestAt;
	private int responseCode;
	private String queryParameters;
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
	
	public String getEnv() {
		return env;
	}
	public void setEnv(String env) {
		this.env = env;
	}
	/**
	 * @return the actionName
	 */
	public String getActionName() {
		return StringUtils.defaultIfBlank(actionName, actionKey);
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
	
	public String getModuleId() {
		return moduleId;
	}
	
	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
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
	
	
	public String getQueryParameters() {
		return queryParameters;
	}
	public void setQueryParameters(String queryParameters) {
		this.queryParameters = queryParameters;
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
	
	public ActionLog apiMeta(ApiInfo apiMeta) {
    	if(apiMeta != null) {
			setActionName(apiMeta.getName());
			if(!apiMeta.isRequestLog())setRequestData(IGNORE_FLAG);
			if(!apiMeta.isResponseLog())setResponseData(IGNORE_FLAG);
		}
    	return this;
    }
    
    public ActionLog apiMeta(ApiMetadata apiMeta) {
    	if(apiMeta != null) {
    		setActionName(apiMeta.actionName());
			if(!apiMeta.requestLog())setRequestData(IGNORE_FLAG);
			if(!apiMeta.responseLog())setResponseData(IGNORE_FLAG);
		}
    	return this;
    }
    
    public ActionLog actionName(String actionName) {
    	setActionName(actionName);
    	return this;
    }
    
    public ActionLog currentUser(AuthUser currentUser) {
    	if(currentUser != null){
			setUserId(currentUser.getId());
			setUserName(currentUser.getName());
		}
    	return this;
    }
    
    public ActionLog exception(Exception e) {
    	setExceptions(StringUtils.defaultIfBlank(e.getMessage(), LogMessageFormat.buildExceptionMessages(e)));
    	setResponseCode(500);
    	return this;
    }
    
    public ActionLog addContext() {
    	ThreadLocalContext.set(ActionLogCollector.CURRENT_LOG_CONTEXT_NAME,this);
    	return this;
    }

}
