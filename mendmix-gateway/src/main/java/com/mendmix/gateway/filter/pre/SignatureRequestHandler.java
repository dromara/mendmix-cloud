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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
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

	private Map<String, String> appIdSecretMappings = new HashMap<String, String>();

	public SignatureRequestHandler() {
		// 本地配置
		Properties properties = ResourceUtils.getAllProperties(GatewayConfigs.OPENAPI_CLIENT_MAPPING_CONFIG_KEY);
		properties.forEach((k, v) -> {
			String appId = k.toString().split("\\[|\\]")[1];
			appIdSecretMappings.put(appId, v.toString());
		});
	}

	@Override
	public Builder process(ServerWebExchange exchange, BizSystemModule module, Builder requestBuilder) {

		ApiInfo apiInfo = module.getApiInfo(exchange.getRequest().getMethodValue(),exchange.getRequest().getPath().value());
		if (apiInfo == null || !apiInfo.isOpenApi()) {
			throw new MendmixBaseException("该接口未开放访问权限");
		}

		HttpHeaders headers = exchange.getRequest().getHeaders();
		String sign = headers.getFirst(GatewayConstants.X_SIGN_HEADER);
		if (StringUtils.isBlank(sign))
			return requestBuilder;
		if (StringUtils.isBlank(sign))
			return requestBuilder;
		String timestamp = headers.getFirst(GatewayConstants.TIMESTAMP_HEADER);
		String appId = headers.getFirst(GatewayConstants.APP_ID_HEADER);

		if (StringUtils.isAnyBlank(timestamp, appId)) {
			throw new MendmixBaseException("认证头信息不完整");
		}

		String secret = appIdSecretMappings.get(appId);

		if (StringUtils.isBlank(secret)) {
			throw new MendmixBaseException("appId不存在");
		}

		Object body = RuequestHelper.getCachingBodyString(exchange);

		Map<String, Object> map = JsonUtils.toHashMap(body.toString(), Object.class);
		String signBaseString = StringUtils.trimToEmpty(ParameterUtils.mapToQueryParams(map)) + timestamp + secret;
		String expectSign = DigestUtils.md5(signBaseString);

		if (!expectSign.equals(sign)) {
			throw new MendmixBaseException("签名错误");
		}

		return requestBuilder;
	}

	@Override
	public int order() {
		return 0;
	}

}
