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
package com.mendmix.logging.actionlog;

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
	
	private String id;
	private String logType;
	private String appId;
	private String env;
	private String tenantId;
	private String clientType;
	private String actionName;
	private String actionKey;
	private String userId;
	private String userName;
	private String moduleId;
	private String clientIp;
	private Date actionAt;
	private String inputData;
	private String outputData;
	private Boolean successed; 
	private Integer useTime;
	private String bizId;
	private String traceId;
	private String exceptions;
	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLogType() {
		return logType;
	}

	public void setLogType(String logType) {
		this.logType = logType;
	}

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

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
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

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public Date getActionAt() {
		return actionAt;
	}

	public void setActionAt(Date actionAt) {
		this.actionAt = actionAt;
	}

	public String getInputData() {
		return inputData;
	}

	public void setInputData(String inputData) {
		this.inputData = inputData;
	}

	public void setFinishAt(Date outputAt) {
		this.useTime = (int) (outputAt.getTime() - actionAt.getTime());
	}

	public String getOutputData() {
		return outputData;
	}

	public void setOutputData(String outputData) {
		this.outputData = outputData;
	}

	public Boolean getSuccessed() {
		return successed;
	}

	public void setSuccessed(Boolean successed) {
		this.successed = successed;
	}

	public Integer getUseTime() {
		return useTime;
	}

	public void setUseTime(Integer useTime) {
		this.useTime = useTime;
	}

	public String getBizId() {
		return bizId;
	}

	public void setBizId(String bizId) {
		this.bizId = bizId;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
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
			if(!apiMeta.isRequestLog())setInputData(IGNORE_FLAG);
			if(!apiMeta.isResponseLog())setOutputData(IGNORE_FLAG);
		}
    	return this;
    }
    
    public ActionLog apiMeta(ApiMetadata apiMeta) {
    	if(apiMeta != null) {
    		setActionName(apiMeta.actionName());
			if(!apiMeta.requestLog())setInputData(IGNORE_FLAG);
			if(!apiMeta.responseLog())setOutputData(IGNORE_FLAG);
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
    	setSuccessed(false);
    	return this;
    }
    
    public ActionLog addContext() {
    	ThreadLocalContext.set(ActionLogCollector.CURRENT_LOG_CONTEXT_NAME,this);
    	return this;
    }

}
