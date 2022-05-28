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

import com.mendmix.common.util.WebUtils;

public class BizSystemPortal {

	private String code;
	private String clientType;
	private String tenantId;
	private String indexPath;
	private String domain;
    
	public String getDomain() {
		if (StringUtils.isBlank(domain) && StringUtils.isNotBlank(indexPath) && indexPath.startsWith("http")) {
			domain = WebUtils.getDomain(indexPath);
		}
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public String getTenantId() {
		return tenantId;
	}
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	public String getClientType() {
		return clientType;
	}
	public void setClientType(String clientType) {
		this.clientType = clientType;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getIndexPath() {
		return indexPath;
	}
	public void setIndexPath(String indexPath) {
		this.indexPath = indexPath;
	}

	
    
}
