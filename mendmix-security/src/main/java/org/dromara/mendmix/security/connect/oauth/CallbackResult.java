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

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Nov 13, 2022
 */
public class CallbackResult {

	private String code;
	private String orginUrl;
	private String redirectUri;
	
	public CallbackResult(String code, String orginUrl, String redirectUri) {
		super();
		this.code = code;
		this.orginUrl = orginUrl;
		this.redirectUri = redirectUri;
	}
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getOrginUrl() {
		return orginUrl;
	}
	public void setOrginUrl(String orginUrl) {
		this.orginUrl = orginUrl;
	}
	public String getRedirectUri() {
		return redirectUri;
	}
	public void setRedirectUri(String redirectUri) {
		this.redirectUri = redirectUri;
	}

	
	
}
