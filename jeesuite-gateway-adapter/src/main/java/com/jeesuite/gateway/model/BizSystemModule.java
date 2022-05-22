package com.jeesuite.gateway.model;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.common.util.PathMatcher;
import com.jeesuite.gateway.GatewayConstants;

public class BizSystemModule {

	private String id;
    private String serviceId;

    private String routeName;

    private String proxyUri;
    
    private String anonymousUris;

    private String systemId;

    private String name;
    
    private boolean global; //
    
    private boolean defaultRoute; //
    
    private boolean bodyRewriteIgnore;
    
    private int stripPrefix = -1;
    
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
		if(serviceId != null)serviceId = serviceId.toLowerCase();
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
	}

	public PathMatcher getAnonUriMatcher() {
		return anonUriMatcher;
	}

	public void setAnonUriMatcher(PathMatcher anonUriMatcher) {
		this.anonUriMatcher = anonUriMatcher;
	}

	public void finalCorrect() {
		if(this.stripPrefix < 0) {
			this.stripPrefix = StringUtils.countMatches(getRouteName(), "/") + 2;
			if(getProxyUri().endsWith("/" + getRouteName())) {
				proxyUri = proxyUri.substring(0,proxyUri.lastIndexOf(getRouteName()) - 1);
				stripPrefix = 1;
			}
		}
		
		if(StringUtils.isNotBlank(this.anonymousUris)) {
			String prefix;
			if(GlobalRuntimeContext.APPID.equals(serviceId)) {
				prefix = GatewayConstants.PATH_PREFIX;
			}else {
				prefix = GatewayConstants.PATH_PREFIX + "/" + routeName;
			}
			anonUriMatcher = new PathMatcher(prefix, this.anonymousUris);
		}
	}

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}
	

	public boolean isDefaultRoute() {
		return defaultRoute;
	}

	public void setDefaultRoute(boolean defaultRoute) {
		this.defaultRoute = defaultRoute;
	}

	public boolean isBodyRewriteIgnore() {
		return bodyRewriteIgnore;
	}

	public void setBodyRewriteIgnore(boolean bodyRewriteIgnore) {
		this.bodyRewriteIgnore = bodyRewriteIgnore;
	}

	public int getStripPrefix() {
		return stripPrefix;
	}

	public void setStripPrefix(int stripPrefix) {
		this.stripPrefix = stripPrefix;
	}

	public String getProxyUri() {
		if(proxyUri == null && serviceId != null && !GlobalRuntimeContext.APPID.equals(serviceId)) {
			if(serviceId.contains(":")) {
				proxyUri = serviceId;
			}else {
				proxyUri = "lb://" + serviceId;
			}
		}
		return proxyUri;
	}

	public void setProxyUri(String proxyUri) {
		this.proxyUri = proxyUri;
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
	
	
	public String getHttpBaseUri() {
		//http://127.0.0.1
		//lb://paas-sysmgt-svc
		//ws://127.0.0.1:8081
		//lb:ws://paas-sysmgt-svc
		if(getProxyUri().contains("ws://")) {
			return null;
		}
		if(getProxyUri().startsWith("http")) {
			return getProxyUri();
		}else if(getProxyUri().contains("lb://")) {
			return getProxyUri().replace("lb://", "http://");
		}
		return null;
	}
	
	public String getMetadataUri() {
		String baseUri = getHttpBaseUri();
		if(baseUri == null) {
			return null;
		}
		return baseUri + "/metadata";
	}
	
	public String getHealthUri() {
		String baseUri = getHttpBaseUri();
		if(baseUri == null) {
			return null;
		}
		return baseUri + "/actuator/health";
	}
	
	public static String resolveApiFinalUri(BizSystemModule module,String uri) {
		if(GlobalRuntimeContext.APPID.equals(module.getRouteName())) {
			return uri;
		}else {
			if(module.getStripPrefix() == 1) {
				uri = GatewayConstants.PATH_PREFIX + uri;
			}else {
				uri = String.format("%s/%s/%s", GatewayConstants.PATH_PREFIX,module.getRouteName(),uri);
			}
		}
		return uri.replace("//", "/");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((serviceId == null) ? 0 : serviceId.toLowerCase().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BizSystemModule other = (BizSystemModule) obj;
		if (serviceId == null) {
			if (other.serviceId != null)
				return false;
		} else if (!serviceId.equalsIgnoreCase(other.serviceId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[routeName=" + routeName + ", serviceId=" + serviceId + ", proxyUri=" + proxyUri
				+ ", stripPrefix=" + stripPrefix + ", anonymousUris=" + anonymousUris + "]";
	}

	
	
}