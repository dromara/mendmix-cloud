package com.jeesuite.cos;

public class BucketConfig {

	private String name;
	private boolean auth;
	private String urlPrefix;
	
	public BucketConfig() {}
	
	public BucketConfig(String name, boolean auth, String urlPrefix) {
		super();
		this.name = name;
		this.auth = auth;
		this.urlPrefix = urlPrefix;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isAuth() {
		return auth;
	}
	public void setAuth(boolean auth) {
		this.auth = auth;
	}
	public String getUrlPrefix() {
		return urlPrefix;
	}
	public void setUrlPrefix(String urlPrefix) {
		this.urlPrefix = urlPrefix;
	}
	
	
}
