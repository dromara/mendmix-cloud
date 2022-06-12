package com.mendmix.common.model;

import com.mendmix.common.http.HttpMethod;

public class ApiModel {

	private String uri;
	private String method;
	
	public ApiModel() {}
	
	
	public ApiModel(HttpMethod method,String uri) {
		super();
		this.uri = uri;
		this.method = method.name();
	}


	public String getUri() {
		return uri;
	}


	public void setUri(String uri) {
		this.uri = uri;
	}


	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	
	
}
