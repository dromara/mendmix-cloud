/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.common.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.model.AuthUser;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Jul 20, 2024
 */
public class ExceptionFormatUtils {
	
	private static boolean filterMode = true;
	private static int showLines = 5;
	private static List<String> filterKeys = Arrays.asList("org.dromara.mendmix","org.apache.ibatis","org.springframework","org.apache.kafka");
	
	public static void setFilterMode(boolean filterMode) {
		ExceptionFormatUtils.filterMode = filterMode;
	}

	public static void setShowLines(int showLines) {
		ExceptionFormatUtils.showLines = showLines;
	}

	public static void setFilterKeys(List<String> filterKeys) {
		ExceptionFormatUtils.filterKeys = filterKeys;
	}

	/**
	 * 生成日志消息
	 * @param actionKey 操作关键词
	 * @param bizKey 业务关键信息如：订单号
	 * @return
	 */
	public static String buildLogHeader(String actionKey,Serializable bizKey) {
		StringBuilder builder = new StringBuilder();
		builder.append(actionKey);
		builder.append("<bizKey:").append(bizKey).append(">");
		String requestId = CurrentRuntimeContext.getRequestId();
		if(requestId != null)builder.append("<requestId:").append(requestId).append(">");
		String tenantId = CurrentRuntimeContext.getTenantId(false);
    	if(tenantId != null)builder.append("<tenantId:").append(tenantId).append(">");
    	AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
    	if(currentUser != null)builder.append("<currentUser:").append(currentUser.getName()).append(">");
    	
		return builder.toString();
	}
	
	
	public static String buildLogTail() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		String requestUrl = ThreadLocalContext.getStringValue(GlobalConstants.CONTEXT_REQUEST_URL_KEY);
		if(requestUrl != null) {
			builder.append("<uri:").append(requestUrl).append(">");
		}
		String systemId = CurrentRuntimeContext.getSystemId();
		if(systemId != null)builder.append("<systemId:").append(systemId).append(">");
		String tenantId = CurrentRuntimeContext.getTenantId(false);
    	if(tenantId != null)builder.append("<tenantId:").append(tenantId).append(">");
    	AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
    	if(currentUser != null)builder.append("<currentUser:").append(currentUser.getName()).append(">");
    	builder.append("]");
    	
		return builder.toString();
	}
	
	
	public static String buildExceptionMessages(Throwable throwable) {
		return buildExceptionMessages(throwable, filterMode);
	}
	
	public static String buildExceptionMessages(Throwable throwable,int showLines) {
		String[] traces = ExceptionUtils.getRootCauseStackTrace(throwable);
    	try {
    		StringBuilder sb = new StringBuilder();
    		showLines = showLines >= traces.length ? traces.length : showLines;
    		for (int i = 0; i < showLines; i++) {
    			sb.append(traces[i]).append("\n");
			}
    		return sb.toString();
		} catch (Exception e) {
			return ExceptionUtils.getStackTrace(e);
		}
	}
	
	public static String buildExceptionMessages(Throwable throwable,boolean filterMode) {
		if (!filterMode) {
			return ExceptionUtils.getStackTrace(throwable);
		}
    	String[] traces = ExceptionUtils.getRootCauseStackTrace(throwable);
    	try {
    		StringBuilder sb = new StringBuilder();
    		for (int i = 0; i < traces.length; i++) {
    			String line = traces[i];
    			if(StringUtils.isBlank(line))continue;
    			if(showLines > i || filterKeys.stream().anyMatch(o -> line.contains(o))) {
    				sb.append(line).append("\n");	
    			}
    			
			}
    		return sb.toString();
		} catch (Exception e) {
			return ExceptionUtils.getStackTrace(e);
		}
    	
    }

	public static Exception wrapExtraExceptionMessages(String sourceName,Exception e) {
		if(e instanceof MendmixBaseException == false 
				|| Thread.currentThread().getName().contains("http-")) {
			return e;
		}
		String requestUrl = ThreadLocalContext.getStringValue(GlobalConstants.CONTEXT_REQUEST_URL_KEY);
	    Map<String, String> contextParam = new HashMap<>(3);
	    contextParam.put("sourceName", sourceName);
	    if(requestUrl != null)contextParam.put("requestUrl", requestUrl);
	    String contextVal = CurrentRuntimeContext.getRequestId();
	    if(contextVal != null)contextParam.put("requestId", contextVal);
	    contextVal = CurrentRuntimeContext.getSystemId();
	    if(contextVal != null)contextParam.put("systemId", contextVal);
	    contextVal = CurrentRuntimeContext.getTenantId();
	    if(contextVal != null)contextParam.put("tenantId", contextVal);
	    contextVal = CurrentRuntimeContext.getContextVal(CustomRequestHeaders.HEADER_SERVICE_CHAIN, false);
	    if(contextVal != null)contextParam.put("serviceChain", contextVal);
	    ((MendmixBaseException)e).setContextParam(contextParam);
	    return e;
	}
}
