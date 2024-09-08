/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package org.dromara.mendmix.logging.reqlog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.model.ApiInfo;
import org.dromara.mendmix.common.model.WrapperResponse;
import org.dromara.mendmix.common.util.ExceptionFormatUtils;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.LogMessageFormat;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.logging.LogConfigs;
import org.dromara.mendmix.logging.LogConstants;
import org.dromara.mendmix.logging.LogKafkaClient;
import org.dromara.mendmix.logging.reqlog.ApiRequestLog.ResponseInfo;
import org.dromara.mendmix.spring.InstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;


/**
 * 日志采集
 * 
 * <br>
 * Class Name   : RequestLogCollector
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年8月22日
 */
public class RequestLogCollector {
	

	private static Logger log = LoggerFactory.getLogger("org.dromara.mendmix");
	
	public static final String CURRENT_API_LOG_CONTEXT_NAME = "ctx_cur_apilog";
	public static final String CURRENT_TRACE_LOG_CONTEXT_NAME = "ctx_cur_tracelog";
	
	private static boolean taskLogEnabled = ResourceUtils.getBoolean("mendmix-cloud.task.log.enabled",true);
	
	private static LogKafkaClient logKafkaClient;
	
	private static String apiLogTopic = ResourceUtils.getProperty("mendmix-cloud.logging.apilog.topicName","mendmix_topic_apiLog");
	
	static {
		if(LogConfigs.API_LOGGING) {
			logKafkaClient = InstanceFactory.getInstance(LogKafkaClient.class);
		}
	}
	
	public static ApiRequestLog onRequestStart(HttpServletRequest request,ApiInfo apiInfo) {
		if(logKafkaClient == null)return null;
		if(request.getHeader(CustomRequestHeaders.HEADER_FORWARDED_GATEWAY) == null 
				&& request.getHeader(CustomRequestHeaders.HEADER_AUTH_USER) == null) {
			return null;
		}
		ApiRequestLog log = ApiRequestLog.build(request,apiInfo);
		ThreadLocalContext.set(CURRENT_API_LOG_CONTEXT_NAME, log);
		return log;
	}
	
	public static ApiRequestLog onRequestStart(ServerHttpRequest request,ApiInfo apiInfo) {
		if(logKafkaClient == null)return null;
		final HttpHeaders headers = request.getHeaders();
		final String userAgent = headers.getFirst(HttpHeaders.USER_AGENT);
		if(GlobalConstants.BACKEND_USER_AGENT_NAME.equals(userAgent)) {
			return null;
		}
		if(!headers.containsKey(HttpHeaders.REFERER)) {
			return null;
		}
		ApiRequestLog log = ApiRequestLog.build(request,apiInfo);
		return log;
	}
	
	public static void setApiLogResponseBody(Object body) {
		if(body == null)return;
		if(logKafkaClient == null)return;
		ApiInfo apiInfo = ThreadLocalContext.get(GlobalConstants.CONTEXT_CURRENT_API_KEY);
		boolean withException = false;
		if(apiInfo != null && !apiInfo.isResponseLog() 
				&& !CurrentRuntimeContext.hasContextVal(CustomRequestHeaders.HEADER_REQUEST_BODY_LOGGING)
				&& !(withException = ThreadLocalContext.exists(GlobalConstants.CONTEXT_EXCEPTION))) {
			return;
		}
		ApiRequestLog requestLog = ThreadLocalContext.get(CURRENT_API_LOG_CONTEXT_NAME);
	    if(requestLog != null) {
	    	ResponseInfo response = requestLog.getResponse();
	    	if(response == null) {
	    		response = new ResponseInfo();
	    		requestLog.setResponse(response);
	    	}
	    	if(withException && body instanceof WrapperResponse) {
	    		WrapperResponse _body = (WrapperResponse) body;
	    		body = WrapperResponse.fail(_body.getCode(), _body.getBizCode(), _body.getMsg());
	    	}
	    	response.body = body;
	    }
	}

	public static void onResponseEnd(ApiRequestLog requestLog,Throwable throwable,int httpStatus,Map<String, String> headers) {
		if(throwable != null) {
			if(httpStatus == 0)httpStatus = 500;
			if (throwable instanceof MendmixBaseException) {
				MendmixBaseException be = (MendmixBaseException)throwable;
				if(be.getCode() != 401 && be.getCode() != 403) {    					
					log.warn("bizError"+LogMessageFormat.buildLogTail()+":{}",ExceptionFormatUtils.buildExceptionMessages(throwable,5));
				}
			}else {
				log.error("systemError"+LogMessageFormat.buildLogTail()+":{}",ExceptionUtils.getStackTrace(throwable));
			}
		}
		//
		if(requestLog != null && httpStatus != 404) {
			requestLog.end(throwable, httpStatus, headers, null);
			if(throwable != null && throwable instanceof MendmixBaseException == false) {
				requestLog.setExceptions(ExceptionFormatUtils.buildExceptionMessages(throwable, 5));
			}
			if(logKafkaClient != null) {
				logKafkaClient.send(apiLogTopic, JsonUtils.toJson(requestLog));
			}
		}
	}
	
	public static void onResponseEnd(HttpServletResponse response,Throwable throwable){
		ApiRequestLog requestLog = ThreadLocalContext.get(CURRENT_API_LOG_CONTEXT_NAME);
		Map<String, String> headers = null;
		if(requestLog != null) {
			Collection<String> headerNames = response.getHeaderNames();
			headers = new HashMap<>(headerNames.size());
	        for (String name : headerNames) {
	        	if(LogConstants.LOGGING_RESPONSE_HEADERS.contains(name)) {	        		
	        		headers.put(name, response.getHeader(name));
	        	}
			}
		}
		onResponseEnd(requestLog, throwable, response.getStatus(), headers);
	}
    
    public static void onSystemBackendTaskStart(String actionGroup,String actionKey,String actionName){
    	if(!taskLogEnabled)return;
    	ApiRequestLog log = ApiRequestLog.build(actionGroup, actionKey, actionName);
    	ThreadLocalContext.set(CURRENT_API_LOG_CONTEXT_NAME, log);
	}
    
    public static void onSystemBackendTaskEnd(Throwable throwable){
    	ApiRequestLog log = ThreadLocalContext.get(CURRENT_API_LOG_CONTEXT_NAME);
    	if(log != null) {
    		onResponseEnd(log, throwable, 0, null);
    	}
    }

    public static void destroy(){}
    
}
