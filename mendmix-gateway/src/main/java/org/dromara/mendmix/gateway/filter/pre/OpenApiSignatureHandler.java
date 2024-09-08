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
package org.dromara.mendmix.gateway.filter.pre;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.model.ApiInfo;
import org.dromara.mendmix.common.util.DigestUtils;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.ParameterUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.gateway.GatewayConfigs;
import org.dromara.mendmix.gateway.GatewayConstants;
import org.dromara.mendmix.gateway.filter.PreFilterHandler;
import org.dromara.mendmix.gateway.helper.RequestContextHelper;
import org.dromara.mendmix.gateway.model.BizSystemModule;
import org.dromara.mendmix.gateway.model.OpenApiConfig;
import org.dromara.mendmix.gateway.security.OpenApiConfigProvider;
import org.dromara.mendmix.spring.InstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

/**
 * 
 * 
 * <br>
 * Class Name : OpenApiSignatureHandler
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2021-04-23
 */
public class OpenApiSignatureHandler implements PreFilterHandler {

	static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.gateway");
	
	private static Map<String, OpenApiConfig> openApiConfigs = new ConcurrentHashMap<>();
	
	private OpenApiConfigProvider configProvider;
	
	public OpenApiSignatureHandler() {
		configProvider = InstanceFactory.getInstance(OpenApiConfigProvider.class);
		// 本地配置
		Properties properties = ResourceUtils.getAllProperties(GatewayConfigs.OPENAPI_CLIENT_MAPPING_CONFIG_KEY);
		properties.forEach((k, v) -> {
			String clientId = k.toString().split("\\[|\\]")[1];
			OpenApiConfig config = new OpenApiConfig(clientId, v.toString());
			if(GatewayConfigs.openApiScopeEnabled) {
				List<String> apis = ResourceUtils.getList("mendmix-cloud.openapi.apiscope.mapping["+clientId+"]");
				config.setGrantedApis(apis);
			}
			openApiConfigs.put(clientId,config);
		});
	}

	
	public OpenApiConfig getOpenApiConfig(String clientId) {
		OpenApiConfig openApiConfig = openApiConfigs.get(clientId);
		if(openApiConfig == null && configProvider != null) {
			openApiConfig = configProvider.openApiConfig(clientId);
			if(openApiConfig != null) {
				openApiConfigs.put(clientId, openApiConfig);
			}
		}
		if(openApiConfig == null) {
			throw new MendmixBaseException("clientId["+clientId+"]配置不存在");
		}
		return openApiConfig;
	}

	@Override
	public Builder process(ServerWebExchange exchange, BizSystemModule module, Builder requestBuilder) {

		HttpHeaders headers = exchange.getRequest().getHeaders();
		String sign = headers.getFirst(CustomRequestHeaders.HEADER_OPEN_SIGN);
		if (StringUtils.isBlank(sign)) {
			return requestBuilder;
		}
		
		final String uri = exchange.getRequest().getPath().value();
		ApiInfo apiInfo = module.getApiInfo(exchange.getRequest().getMethodValue(),uri);
		if (apiInfo == null || !apiInfo.isOpenApi()) {
			throw new MendmixBaseException(500,"该接口未开放访问权限");
		}
		String timestamp = validateTimeStamp(headers);
		String clientId = headers.getFirst(CustomRequestHeaders.HEADER_OPEN_APP_ID);
		if (StringUtils.isBlank(clientId)) {
			throw new MendmixBaseException(400,"请求头[x-open-clientId]缺失");
		}
		String requestId = exchange.getRequest().getHeaders().getFirst(CustomRequestHeaders.HEADER_REQUEST_ID);
        if(StringUtils.isBlank(requestId)) {
        	throw new MendmixBaseException("请求头[x-request-id]缺失");
        }

		OpenApiConfig openApiConfig = getOpenApiConfig(clientId);

		Object body = RequestContextHelper.getCachingBodyString(exchange);
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
		
		String signBaseString = StringUtils.trimToEmpty(ParameterUtils.mapToQueryParams(map)) + timestamp + requestId + openApiConfig.getClientSecret();
		String expectSign = DigestUtils.md5(signBaseString);

		if (!expectSign.equals(sign)) {
			throw new MendmixBaseException(400,"签名错误");
		}
		
		if(GatewayConfigs.openApiScopeEnabled) {
			String removeContextPathUri = uri.substring(GatewayConfigs.PATH_PREFIX.length());
			if(openApiConfig.getGrantedApis() == null || !openApiConfig.getGrantedApis().contains(removeContextPathUri)) {
				logger.info("MENDMIX-TRACE-LOGGGING-->> openapi_error_apiUnauthorized -> clientId:{},uri:{}",openApiConfig.getClientId(),apiInfo.getUri());
				throw new MendmixBaseException(403,"未开通该接口访问权限");
			}
		}
		
		ThreadLocalContext.set(GatewayConstants.CONTEXT_TRUSTED_REQUEST, Boolean.TRUE);
		return requestBuilder;
	}
	
	private String validateTimeStamp(HttpHeaders headers) {
		String timestamp = headers.getFirst(CustomRequestHeaders.HEADER_TIMESTAMP);
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
		if(configProvider == null)return;
		logger.info("MENDMIX-TRACE-LOGGGING-->> init OpenApiConfigs begin...");
		List<OpenApiConfig> configs = configProvider.allOpenApiConfigs();
		if(configs != null) {
			for (OpenApiConfig config : configs) {
				openApiConfigs.put(config.getClientId(), config);
			}
		}
		logger.info("MENDMIX-TRACE-LOGGGING-->> init OpenApiConfigs finish -> clientIds:{}",openApiConfigs.keySet());
	}
	
	

}
