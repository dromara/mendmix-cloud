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
package org.dromara.mendmix.security.connect.oauth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.util.UriComponentsBuilder;

import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.security.SecurityDelegating;
import org.dromara.mendmix.security.model.UserSession;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Oct 31, 2022
 */
@Controller
public class OauthEndpointController implements InitializingBean {

	private static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.security");
	
	private Map<String, OauthClient> oauth2Configs = new HashMap<>();
	
	@Autowired
	private OauthNextHandler<? extends AuthUser> oauthNextHandler;

	@GetMapping("/oauth2/{type}/redirect")
	public Object oauth2Redirect(ServerHttpRequest request,@PathVariable("type") String type) {
		OauthClient client = oauth2Configs.get(type);
		return Rendering.redirectTo(client.onceAuthorizeUrl(request)).build();
	}
	
	@GetMapping("/oauth2/{type}/callback")
	public Rendering oauth2Endpoint(ServerHttpRequest request,@PathVariable("type") String type) {
		
		String redirectUrl = null;
		try {
			OauthClient client = oauth2Configs.get(type);
			CallbackResult callbackResult = client.validateRedirectRequest(request);
			redirectUrl = callbackResult.getOrginUrl();
			Map<String, Object> openUserInfo = client.exchange(request,callbackResult);
			//
			OauthNextResult<? extends AuthUser> oauthNextResult = oauthNextHandler.handle(type, openUserInfo);
			//
			if(oauthNextResult.getRedirectUrl() != null) {
				redirectUrl = oauthNextResult.getRedirectUrl();
			}
		    if(oauthNextResult.getUser() != null) {
		    	UserSession session = SecurityDelegating.updateSession(oauthNextResult.getUser(), true);
		    	redirectUrl = redirectUrl + (redirectUrl.contains("?") ? "&" : "?") + "sessionId" + session.getSessionId();
		    }
		} catch (Exception e) {
			logger.error("oauth2callback_error",e);
			if(redirectUrl == null)request.getHeaders().getFirst(HttpHeaders.REFERER);
			redirectUrl = UriComponentsBuilder.fromHttpUrl(redirectUrl).replaceQuery("error=" + e.getMessage()).build().encode().toUriString();
		}
	    
	    return Rendering.redirectTo(redirectUrl).build();
		
	}

	
	@Override
	public void afterPropertiesSet() throws Exception {
		Set<String> types = ResourceUtils.getMappingValues("mendmix-cloud.security.oauth").keySet();
		
		OauthClient client;
		for (String type : types) {
			client = new OauthClient();
			client.setType(type);
			client.setClientId(ResourceUtils.getAndValidateProperty(String.format("mendmix-cloud.security.oauth[%s].clientId", type)));
			client.setClientSecret(ResourceUtils.getAndValidateProperty(String.format("mendmix-cloud.security.oauth[%s].clientSecret", type)));
			client.setAuthorizeUrl(ResourceUtils.getAndValidateProperty(String.format("mendmix-cloud.security.oauth[%s].authorizeUrl", type)));
			client.setAccessTokenUrl(ResourceUtils.getAndValidateProperty(String.format("mendmix-cloud.security.oauth[%s].accessTokenUrl", type)));
			client.setUserInfoUrl(ResourceUtils.getAndValidateProperty(String.format("mendmix-cloud.security.oauth[%s].userInfoUrl", type)));
			client.afterPropertiesSet();
			oauth2Configs.put(type, client);
		}
		
	}

}
