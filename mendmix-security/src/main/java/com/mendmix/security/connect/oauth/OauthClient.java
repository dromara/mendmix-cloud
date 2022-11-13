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
package com.mendmix.security.connect.oauth;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.http.HttpResponseEntity;
import com.mendmix.common.util.HttpUtils;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.common.util.TokenGenerator;
import com.mendmix.security.SecurityDelegating;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Nov 1, 2022
 */
public class OauthClient {
	
	private static Map<String, String> aliasMapping = new HashMap<>();
	
	private String type;
	private String clientId;
	private String clientSecret;
	private String authorizeUrl;
	private String accessTokenUrl;
	private String userInfoUrl;

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String getClientSecret() {
		return clientSecret;
	}
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	public String getAuthorizeUrl() {
		return authorizeUrl;
	}
	public void setAuthorizeUrl(String authorizeUrl) {
		this.authorizeUrl = authorizeUrl;
	}
	public String getAccessTokenUrl() {
		return accessTokenUrl;
	}
	public void setAccessTokenUrl(String accessTokenUrl) {
		this.accessTokenUrl = accessTokenUrl;
	}
	public String getUserInfoUrl() {
		return userInfoUrl;
	}
	public void setUserInfoUrl(String userInfoUrl) {
		this.userInfoUrl = userInfoUrl;
	}

	public String onceAuthorizeUrl(ServerHttpRequest request) {
		String url = authorizeUrl;
		//oauth2/{type}/callback
		String redirectUri = request.getURI().toString().replace("/redirect", "/callback");
		url = url.replace(OauthParameter.redirectUri.getExpression(), redirectUri);
		//
		String state = TokenGenerator.generate();
		url = url.replace(OauthParameter.state.getExpression(), state);
		String referer = request.getHeaders().getFirst(HttpHeaders.REFERER);
		SecurityDelegating.setTemporaryState(state, referer + "|" + redirectUri);
		return url;
	}
	
	public String onceParameterValue(ServerHttpRequest request,OauthParameter parameter) {
		return request.getQueryParams().getFirst(parameter.name());
	}
	
	public CallbackResult validateRedirectRequest(ServerHttpRequest request) {
		String errorMsg = request.getQueryParams().getFirst(getOauthField(OauthStandardField.errMsg));
		if(errorMsg != null) {
			throw new MendmixBaseException(errorMsg);
		}
		String code = request.getQueryParams().getFirst(OauthParameter.code.name());
		if(StringUtils.isBlank(code)) {
			throw new MendmixBaseException("Parameter[code] is required");
		}
		String state = request.getQueryParams().getFirst(OauthParameter.state.name());
		if(StringUtils.isBlank(state)) {
			throw new MendmixBaseException("Parameter[state] is required");
		}
		String stateValue = SecurityDelegating.getTemporaryState(state);
		//
		if(stateValue == null) {
			throw new MendmixBaseException("Parameter[state] is invalid");
		}
		String[] values = StringUtils.split(stateValue, "|");
		return new CallbackResult(code, values[0],values[1]);
	}
	
	public Map<String, Object> exchange(ServerHttpRequest request,CallbackResult callback) {
		//code -> accesstoken
		String url = accessTokenUrl;
		url = url.replace(OauthParameter.code.getExpression(), callback.getCode()) //
				 .replace(OauthParameter.redirectUri.getExpression(), callback.getRedirectUri());
		HttpResponseEntity responseEntity = HttpUtils.get(url);
		String accessToken = parseHttpResponse(responseEntity).get(getOauthField(OauthStandardField.access_token)).toString();
		//accesstoken -> user
		url = userInfoUrl.replace(OauthParameter.access_token.getExpression(), accessToken);
		responseEntity = HttpUtils.get(url);
		return parseHttpResponse(responseEntity);
	}
	
	
	public void afterPropertiesSet() {
		authorizeUrl = authorizeUrl.replace(OauthParameter.clientId.getExpression(), clientId);
		accessTokenUrl = accessTokenUrl.replace(OauthParameter.clientId.getExpression(), clientId);
		accessTokenUrl = accessTokenUrl.replace(OauthParameter.clientSecret.getExpression(), clientSecret);
		userInfoUrl = userInfoUrl.replace(OauthParameter.clientId.getExpression(), clientId);
		userInfoUrl = userInfoUrl.replace(OauthParameter.clientSecret.getExpression(), clientSecret);
		
		String aliasProps = ResourceUtils.getProperty(String.format("mendmix.security.oauth[%s].aliasMapping", type));
		if(StringUtils.isNotBlank(aliasProps)) {
			String[] pairs = StringUtils.split(aliasProps, ",");
			for (String pair : pairs) {
				String[] parts = StringUtils.split(pair, "=");
				aliasMapping.put(parts[0], parts[1]);
			}
		}
	}
	
	private Map<String,Object> parseHttpResponse(HttpResponseEntity responseEntity){
		if(!responseEntity.isSuccessed()) {
			throw new MendmixBaseException(responseEntity.getStatusCode(), responseEntity.getMessage());
		}
		Map<String, Object> map = JsonUtils.toHashMap(responseEntity.getBody());
		String errorMsgField = getOauthField(OauthStandardField.errMsg);
		if(map.containsKey(errorMsgField)) {
			String msg = map.get(errorMsgField).toString();
			throw new MendmixBaseException(msg);
		}
		return map;
	}
	
	private String getOauthField(OauthStandardField field) {
		if(aliasMapping.containsKey(field.name())) {
			return aliasMapping.get(field.name());
		}
		return field.name();
	}

}
