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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.model.ApiInfo;
import com.mendmix.common.util.DigestUtils;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.ParameterUtils;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.gateway.GatewayConfigs;
import com.mendmix.gateway.GatewayConstants;
import com.mendmix.gateway.filter.PreFilterHandler;
import com.mendmix.gateway.helper.RuequestHelper;
import com.mendmix.gateway.model.BizSystemModule;
import com.mendmix.gateway.model.OpenApiConfig;
import com.mendmix.gateway.security.OpenApiConfigProvider;
import com.mendmix.logging.integrate.ActionLog;
import com.mendmix.logging.integrate.ActionLogCollector;
import com.mendmix.spring.InstanceFactory;

/**
 * 
 * 
 * <br>
 * Class Name : SignatureRequestHandler
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2021-04-23
 */
public class SignatureRequestHandler implements PreFilterHandler {

	static Logger logger = LoggerFactory.getLogger("com.mendmix.gateway");
	
	private static Map<String, OpenApiConfig> openApiConfigs = new HashMap<>();
	
	private static OpenApiConfigProvider configProvider;
	
	private static OpenApiConfigProvider getConfigProvider() {
		if(configProvider != null)return configProvider;
		synchronized (openApiConfigs) {
			if(configProvider != null)return configProvider;
			configProvider = InstanceFactory.getInstance(OpenApiConfigProvider.class);
            if(configProvider == null) {
            	configProvider = new OpenApiConfigProvider() {
					@Override
					public OpenApiConfig openApiConfig(String clientId) {
						return null;
					}
					@Override
					public List<OpenApiConfig> allOpenApiConfigs() {
						
						List<OpenApiConfig> configs = new ArrayList<OpenApiConfig>();
						// 本地配置
						Properties properties = ResourceUtils.getAllProperties(GatewayConfigs.OPENAPI_CLIENT_MAPPING_CONFIG_KEY);
						properties.forEach((k, v) -> {
							String clientId = k.toString().split("\\[|\\]")[1];
							OpenApiConfig config = new OpenApiConfig(clientId, v.toString());
							if(GatewayConfigs.openApiScopeEnabled) {
								List<String> apis = ResourceUtils.getList("mendmix.openapi.apiscope.mapping["+clientId+"]");
								config.setGrantedApis(apis);
							}
							configs.add(config);
						});
						return configs;
					}
				};
            }
		}
		return configProvider;
	}
	
	public static OpenApiConfig getOpenApiConfig(String clientId) {
		OpenApiConfig openApiConfig = openApiConfigs.get(clientId);
		if(openApiConfig == null) {
			openApiConfig = getConfigProvider().openApiConfig(clientId);
		}
		if(openApiConfig == null) {
			throw new MendmixBaseException("clientId["+clientId+"]配置不存在");
		}
		return openApiConfig;
	}

	@Override
	public Builder process(ServerWebExchange exchange, BizSystemModule module, Builder requestBuilder) {

		HttpHeaders headers = exchange.getRequest().getHeaders();
		String sign = headers.getFirst(GatewayConstants.X_SIGN_HEADER);
		if (StringUtils.isBlank(sign)) {
			return requestBuilder;
		}
		
		ApiInfo apiInfo = module.getApiInfo(exchange.getRequest().getMethodValue(),exchange.getRequest().getPath().value());
		if (apiInfo == null || !apiInfo.isOpenApi()) {
			throw new MendmixBaseException(500,"该接口未开放访问权限");
		}
		String timestamp = headers.getFirst(GatewayConstants.TIMESTAMP_HEADER);
		String clientId = headers.getFirst(GatewayConstants.APP_ID_HEADER);

		if (StringUtils.isAnyBlank(timestamp, clientId)) {
			throw new MendmixBaseException(400,"认证头信息不完整");
		}

		OpenApiConfig openApiConfig = getOpenApiConfig(clientId);

		Object body = RuequestHelper.getCachingBodyString(exchange);
		Map<String, Object> map = JsonUtils.toHashMap(body.toString(), Object.class);
		if(map == null)map = new HashMap<>();
		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
		for (Entry<String, List<String>> entry : queryParams.entrySet()) {
			if(entry.getValue() == null)continue;
			if(entry.getValue().size() == 1) {
				map.put(entry.getKey(), entry.getValue().get(0));
			}else if(entry.getValue().size() > 1) {
				map.put(entry.getKey(), entry.getValue());
			}
		}
		
		String signBaseString = StringUtils.trimToEmpty(ParameterUtils.mapToQueryParams(map)) + timestamp + openApiConfig.getClientSecret();
		String expectSign = DigestUtils.md5(signBaseString);

		if (!expectSign.equals(sign)) {
			throw new MendmixBaseException(400,"签名错误");
		}
		
		if(GatewayConfigs.openApiScopeEnabled) {
			if(openApiConfig.getGrantedApis() == null || !openApiConfig.getGrantedApis().contains(apiInfo.getUri())) {
				logger.info("openapi_error_apiUnauthorized -> clientId:{},uri:{}",openApiConfig.getClientId(),apiInfo.getUri());
				throw new MendmixBaseException(403,"未开通该接口访问权限");
			}
		}
		
		ActionLog actionLog = exchange.getAttribute(ActionLogCollector.CURRENT_LOG_CONTEXT_NAME);
		if(actionLog != null) {
			actionLog.setUserId(clientId);
			actionLog.setUserName(clientId);
			actionLog.setClientType("openApi");
		}

		return requestBuilder;
	}

	@Override
	public int order() {
		return 0;
	}

	@Override
	public void onStarted() {
		logger.info("init OpenApiConfigs begin...");
		List<OpenApiConfig> configs = getConfigProvider().allOpenApiConfigs();
		for (OpenApiConfig config : configs) {
			openApiConfigs.put(config.getClientId(), config);
		}
		logger.info("init OpenApiConfigs finish -> clientIds:{}",openApiConfigs.keySet());
	}
	
	

}
