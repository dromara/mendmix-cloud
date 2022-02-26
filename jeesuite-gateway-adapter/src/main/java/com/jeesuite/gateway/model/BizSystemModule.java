package com.jeesuite.gateway.model;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.common.util.PathMatcher;

public class BizSystemModule {

	private String id;
    private String serviceId;
    private String proxyUrl;

    private String routeName;

    private String anonymousUris;
    
    @JsonIgnore
    private List<String> activeNodes;
    
    @JsonIgnore
    private PathMatcher anonUriMatcher;
    
    private Map<String, ApiInfo> apiInfos;


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	
	public String getProxyUrl() {
		return proxyUrl;
	}

	public void setProxyUrl(String proxyUrl) {
		this.proxyUrl = proxyUrl;
	}

	public String getRouteName() {
		return routeName;
	}

	public void setRouteName(String routeName) {
		this.routeName = routeName;
	}
	

	public List<String> getActiveNodes() {
		return activeNodes;
	}

	public void setActiveNodes(List<String> activeNodes) {
		this.activeNodes = activeNodes;
	}

	public String getAnonymousUris() {
		return anonymousUris;
	}

	public void setAnonymousUris(String anonymousUris) {
		this.anonymousUris = anonymousUris;
	}


	public PathMatcher getAnonUriMatcher() {
		return anonUriMatcher;
	}

	public void setAnonUriMatcher(PathMatcher anonUriMatcher) {
		this.anonUriMatcher = anonUriMatcher;
	}
	
	public void setApiInfos(Map<String, ApiInfo> apiInfos) {
		this.apiInfos = apiInfos;
	}
	
	public Map<String, ApiInfo> getApiInfos() {
		return apiInfos;
	}

	public ApiInfo getApiInfo(String uri) {
		return apiInfos == null ? null : apiInfos.get(uri);
	}

	public void buildAnonUriMatcher() {
		if(StringUtils.isNotBlank(this.anonymousUris)) {
			String prefix;
			if(GlobalRuntimeContext.APPID.equals(serviceId)) {
				prefix = GlobalRuntimeContext.getContextPath();
			}else {
				prefix = GlobalRuntimeContext.getContextPath() + "/" + routeName;
			}
			anonUriMatcher = new PathMatcher(prefix, this.anonymousUris);
		}
	}
    
}