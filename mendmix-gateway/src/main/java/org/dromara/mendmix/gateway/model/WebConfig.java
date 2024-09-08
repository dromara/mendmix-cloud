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
package org.dromara.mendmix.gateway.model;

import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.gateway.helper.RSAKeyPairHolder;

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
			WebConfig _config = new WebConfig();
			_config.systemKey = GlobalContext.SYSTEM_KEY;
			_config.setPublicKey(RSAKeyPairHolder.getPublicKeyString());
			config = _config;
		}
		return config;
	}
	
	private String systemKey;
	private String publicKey;
	
	public String getSystemKey() {
		return systemKey;
	}
	
	public String getPublicKey() {
		return publicKey;
	}
	
	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

	
}
