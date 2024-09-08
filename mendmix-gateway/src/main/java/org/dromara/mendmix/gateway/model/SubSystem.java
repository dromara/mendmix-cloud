package org.dromara.mendmix.gateway.model;

public class SubSystem {

	private String id;
	private String code;
	private boolean routeMode;
	private String proxyUrl;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public boolean isRouteMode() {
		return routeMode;
	}
	public void setRouteMode(boolean routeMode) {
		this.routeMode = routeMode;
	}
	public String getProxyUrl() {
		return proxyUrl;
	}
	public void setProxyUrl(String proxyUrl) {
		this.proxyUrl = proxyUrl;
	}
}
