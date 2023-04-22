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
package com.mendmix.amqp.logging;

import java.util.Date;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.mendmix.amqp.MQContext.ActionType;
import com.mendmix.amqp.MQLogHandler;
import com.mendmix.amqp.MQMessage;
import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.util.IpUtils;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.logging.actionlog.ActionLog;
import com.mendmix.logging.actionlog.ActionLogType;
import com.mendmix.logging.actionlog.LogStorageProvider;
import com.mendmix.logging.actionlog.storage.HttpApiLogStorageProvider;
import com.mendmix.spring.InstanceFactory;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Sep 17, 2022
 */
public class DefaultMQLogHandler implements MQLogHandler {

	private boolean inited = false;
	private LogStorageProvider logStorageProvider;
	
	private LogStorageProvider getLogStorageProvider() {
		if(inited || logStorageProvider != null)return logStorageProvider;
		synchronized (this) {
			if(logStorageProvider != null)return logStorageProvider;
			logStorageProvider = InstanceFactory.getInstance(LogStorageProvider.class);
			if(logStorageProvider == null && ResourceUtils.containsProperty("mendmix.actionlog.api.baseUrl")) {
				logStorageProvider = new HttpApiLogStorageProvider(ResourceUtils.getProperty("mendmix.actionlog.api.baseUrl"));
			}
			inited = true;
		}
		return logStorageProvider;
	}


	@Override
	public void onSuccess(String groupName, ActionType actionType, MQMessage message) {
		if(getLogStorageProvider() == null)return;
		ActionLog actionLog = buildActionLogObject(groupName, actionType, message);
		actionLog.setSuccessed(true);
		logStorageProvider.storage(actionLog);
	}


	@Override
	public void onError(String groupName, ActionType actionType, MQMessage message, Throwable e) {
		if(getLogStorageProvider() == null)return;
		ActionLog actionLog = buildActionLogObject(groupName, actionType, message);
		actionLog.setSuccessed(false);
		actionLog.setExceptions(ExceptionUtils.getStackTrace(e));
		logStorageProvider.storage(actionLog);
	}
	
	private ActionLog buildActionLogObject(String groupName, ActionType actionType, MQMessage message) {
		ActionLog actionLog = new ActionLog();
		actionLog.setLogType(ActionLogType.messageQueue.name());
		actionLog.setSystemKey(GlobalRuntimeContext.SYSTEM_KEY);
		actionLog.setModuleKey(GlobalRuntimeContext.APPID);
		actionLog.setEnv(GlobalRuntimeContext.ENV);
		actionLog.setActionAt(new Date());
		actionLog.setTraceId(CurrentRuntimeContext.getRequestId());
		actionLog.setActionName(actionType == ActionType.sub ? "messageConsume" : "messageProduce");
		actionLog.setActionKey(actionType.name() + "_" + message.getTopic());
		if(actionType == ActionType.sub) {
			actionLog.setInputData(message.toMessageValue(true));
		}
		actionLog.setFinishAt(actionLog.getActionAt());
		actionLog.setClientIp(IpUtils.getLocalIpAddr());
		actionLog.setUserId(groupName);
		actionLog.setUserName(groupName);
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
		if(currentUser != null){
			actionLog.setUserId(currentUser.getId());
			actionLog.setUserName(currentUser.getName());
		}
		actionLog.setTenantId(CurrentRuntimeContext.getTenantId());
		return actionLog;
	}

}
