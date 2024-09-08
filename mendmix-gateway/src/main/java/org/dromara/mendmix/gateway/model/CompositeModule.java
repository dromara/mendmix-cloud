package org.dromara.mendmix.gateway.model;

import java.util.List;

public class CompositeModule {

	private String systemId;
	private String systemKey;
    private String proxyUri;
    private List<String> routes;
    
	public String getSystemId() {
		return systemId;
	}
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}
	public String getSystemKey() {
		return systemKey;
	}
	public void setSystemKey(String systemKey) {
		this.systemKey = systemKey;
	}
	public String getProxyUri() {
		return proxyUri;
	}
	public void setProxyUri(String proxyUri) {
		this.proxyUri = proxyUri;
	}
	public List<String> getRoutes() {
		return routes;
	}
	public void setRoutes(List<String> routes) {
		this.routes = routes;
	}
    
    
}
