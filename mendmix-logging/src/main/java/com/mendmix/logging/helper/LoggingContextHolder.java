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
package com.mendmix.logging.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.util.TokenGenerator;
import com.mendmix.logging.integrate.LogConstants;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Jul 30, 2022
 */
public class LoggingContextHolder {

	
	public static void initRequestContext(String ipAddr){
		if(ipAddr != null) {
			ThreadContext.put(LogConstants.LOG_CONTEXT_REQUEST_IP, ipAddr);
		}
		String value = CurrentRuntimeContext.getRequestId();
		if(StringUtils.isBlank(value)){
			value = TokenGenerator.generate();
		}
		ThreadContext.put(LogConstants.LOG_CONTEXT_REQUEST_ID, value);
		
		value = CurrentRuntimeContext.getInvokeAppId();
		if(StringUtils.isNotBlank(value)){
			ThreadContext.put(LogConstants.LOG_CONTEXT_INVOKER_APP_ID, value);
		}
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
		if(currentUser != null){
			ThreadContext.put(LogConstants.LOG_CONTEXT_CURRENT_USER, currentUser.getName());
		}
	}
}
