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
package com.mendmix.gateway.filter.pre;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.web.server.ServerWebExchange;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.CustomRequestHeaders;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.constants.PermissionLevel;
import com.mendmix.common.model.ApiInfo;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.util.DigestUtils;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.gateway.GatewayConfigs;
import com.mendmix.gateway.GatewayConstants;
import com.mendmix.gateway.filter.PreFilterHandler;
import com.mendmix.gateway.helper.RequestContextHelper;
import com.mendmix.gateway.model.BizSystemModule;
import com.mendmix.gateway.model.WebConfig;

public class WebApiSignatureHandler implements PreFilterHandler {
	
	private static Logger logger = LoggerFactory.getLogger("com.zvosframework.adapter.gateway");
	
	private List<String> ignoreUris = ResourceUtils.getList("application.webrequest.sign.ignore.uris");
	
	@Override
	public Builder process(ServerWebExchange exchange,BizSystemModule module,Builder reqBuilder) {

		if(!ignoreUris.isEmpty() && ignoreUris.contains(exchange.getRequest().getPath().value())) {
			return reqBuilder;
		}
		
		if(RequestContextHelper.isMultipartContent(exchange.getRequest())) {
	    	return reqBuilder;
	    }
		
		if(ThreadLocalContext.get(GatewayConstants.CONTEXT_TRUSTED_REQUEST, false)) {
			return reqBuilder;
		}
		
		ApiInfo apiInfo = RequestContextHelper.getCurrentApi(exchange);
        if(apiInfo != null && apiInfo.getPermissionLevel() == PermissionLevel.Anonymous) {
        	return reqBuilder;
        }
        
        String query = exchange.getRequest().getURI().getQuery();
        String body = RequestContextHelper.getCachingBodyString(exchange);
        
		String timeStamp = validateTimeStamp(exchange.getRequest().getHeaders());
		
		String sign = exchange.getRequest().getHeaders().getFirst(GatewayConstants.REQ_SIGN_HEADER);
        if(StringUtils.isBlank(sign)) {
        	throw new MendmixBaseException("请求头[x-sign]缺失");
        }
        //
        String requestId = exchange.getRequest().getHeaders().getFirst(CustomRequestHeaders.HEADER_REQUEST_ID);
        if(StringUtils.isBlank(requestId)) {
        	throw new MendmixBaseException("请求头[x-request-id]缺失");
        }
		
        StringBuilder builder = new StringBuilder();
		if(StringUtils.isNotBlank(query)) {
			builder.append(query);
		}
		if(StringUtils.isNotBlank(body)) {
			builder.append(body);
		}
		builder.append(timeStamp);
		AuthUser authUser = CurrentRuntimeContext.getCurrentUser();
		if(authUser != null) {
			builder.append(authUser.getId());
		}
		builder.append(WebConfig.getDegault().getSafeSignSalt());
		builder.append(requestId);
		
		String signPlainText = builder.toString();
		String expectSign = DigestUtils.md5(signPlainText);
		if (!expectSign.equals(sign)) {
			logger.info("ZVOS-FRAMEWORK-TRACE-LOGGGING-->> sign_error sign:{}\nsignContent:{}\nexpectSign:{}",sign,signPlainText,expectSign);
			throw new MendmixBaseException(400,"error.sign.notMatch","签名错误");
		}

		return reqBuilder;
	}

	private String validateTimeStamp(HttpHeaders headers) {
		String timestamp = headers.getFirst(GatewayConstants.TIMESTAMP_HEADER);
		if (StringUtils.isBlank(timestamp)) {
			throw new MendmixBaseException("请求头[timestamp]缺失");
		}

		long diff = Math.abs(System.currentTimeMillis() - Long.parseLong(timestamp));
		if (diff > GatewayConfigs.REQUEST_TIME_OFFSET_THRESHOLD) {
			throw new MendmixBaseException("timestamp范围失效");
		}
		return timestamp;
	}

	@Override
	public int order() {
		return 1;
	}

	@Override
	public void onStarted() {
		WebConfig.getDegault();
	}
	
	
}
