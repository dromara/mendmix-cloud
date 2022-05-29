package com.mendmix.common.model;

public class ApiModel {

	private String uri;
	private String method;
	
	public ApiModel() {}
	
	
	public ApiModel(String uri, String method) {
		super();
		this.uri = uri;
		this.method = method;
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
