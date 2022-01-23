package com.jeesuite.zuul.model;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.util.WebUtils;

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
