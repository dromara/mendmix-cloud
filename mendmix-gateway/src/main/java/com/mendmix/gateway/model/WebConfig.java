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
package com.mendmix.gateway.model;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.util.DigestUtils;
import com.mendmix.gateway.GatewayConfigs;
import com.mendmix.gateway.helper.RSAKeyPairHolder;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年6月22日
 */
public class WebConfig {
	
	private static WebConfig config;
	
	public static WebConfig getDegault() {
		if(config == null) {
			config = new WebConfig();
			config.setApiSign(GatewayConfigs.WEB_REQUEST_SIGN_ENABLED);
			config.setPublicKey(RSAKeyPairHolder.getPublicKeyString());
			config.signSalt = DigestUtils.md5(GlobalRuntimeContext.ENV + GlobalRuntimeContext.SYSTEM_ID);
			config.safeSignSalt = "mendmix@" + StringUtils.reverse(config.signSalt);
		}
		return config;
	}
	private boolean apiSign;
	private String signSalt;
	private String publicKey;
	@JsonIgnore
	private String safeSignSalt;
	
	public boolean isApiSign() {
		return apiSign;
	}
	public void setApiSign(boolean apiSign) {
		this.apiSign = apiSign;
	}
	public String getSignSalt() {
		return signSalt;
	}
	
	public String getPublicKey() {
		return publicKey;
	}
	
	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
	
	public String getSafeSignSalt() {
		return safeSignSalt;
	}
	
	
}
