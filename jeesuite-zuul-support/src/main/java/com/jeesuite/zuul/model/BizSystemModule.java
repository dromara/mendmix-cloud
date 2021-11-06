package com.jeesuite.zuul.model;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.util.PathMatcher;

public class BizSystemModule {

	private String id;
    private String serviceId;

    private String routeName;

    private String anonymousUris;
    
    @JsonIgnore
    private List<String> activeNodes;
    
    @JsonIgnore
    private PathMatcher anonUriMatcher;


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
		if(StringUtils.isNotBlank(this.anonymousUris)) {
			anonUriMatcher = new PathMatcher(GlobalRuntimeContext.getContextPath(), this.anonymousUris);
		}
	}

	public PathMatcher getAnonUriMatcher() {
		return anonUriMatcher;
	}

	public void setAnonUriMatcher(PathMatcher anonUriMatcher) {
		this.anonUriMatcher = anonUriMatcher;
	}

    
}