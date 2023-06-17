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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mendmix.common.GlobalConstants;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.model.ApiInfo;
import com.mendmix.gateway.GatewayConfigs;

public class BizSystemModule {

	private String id;
    private String serviceId;

    private String routeName;

    private String proxyUri;
    
    private String systemId;

    private String name;
    
    private boolean global; //
    
    private boolean defaultRoute; //
    
    private int stripPrefix = -1;
    
    private String serviceBaseUrl;
    
    private String uriPrefix;

    @JsonIgnore
    private Map<Pattern, ApiInfo> wildcardUris = new HashMap<>();
    @JsonIgnore
    private List<Pattern> sortedWildcardPatterns = new ArrayList<>();


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
	

	public void format() {
		if (this.stripPrefix < 0) {
			//route:spu,proxyUri:lb://mall-product-svc
			//route:spu,proxyUri:lb://mall-product-svc/spu
			//route:mall/spu,proxyUri:lb://mall-product-svc/spu
			this.stripPrefix = StringUtils.countMatches(getRouteName(), "/") + 2;
			if(StringUtils.countMatches(getProxyUri(), "/") > 2) {
				String removeSchema = StringUtils.splitByWholeSeparator(proxyUri, "://")[1];
				String path = removeSchema.substring(removeSchema.indexOf("/"));
				this.stripPrefix = this.stripPrefix - StringUtils.countMatches(getRouteName().replace(path, ""), "/") - 1;
			}
		}
		uriPrefix = GatewayConfigs.PATH_PREFIX + "/" + getRouteName();
		//
		if(!isGateway() && !getProxyUri().contains("ws://")) {
			if(getProxyUri().startsWith("http")) {
				serviceBaseUrl = getProxyUri();
			}else if(getProxyUri().startsWith("lb://")) {
				serviceBaseUrl = getProxyUri().replace("lb://", "http://");
			}
			if(serviceBaseUrl != null && stripPrefix == 0) {
				if(serviceBaseUrl.endsWith(GatewayConfigs.PATH_PREFIX)) {
					serviceBaseUrl = serviceBaseUrl + "/" + routeName;
				}else {
					serviceBaseUrl = serviceBaseUrl + GatewayConfigs.PATH_PREFIX + "/" + routeName;
				}
			}
		}
		//
		if(!isGateway()){
			URI uri = URI.create(getProxyUri());
			this.proxyUri = uri.getScheme() + "://" + uri.getAuthority();
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

	public int getStripPrefix() {
		return stripPrefix;
	}

	public void setStripPrefix(int stripPrefix) {
		this.stripPrefix = stripPrefix;
	}

	public String getProxyUri() {
		if(proxyUri == null && serviceId != null && !isGateway()) {
			if(serviceId.contains(":")) {
				proxyUri = serviceId;
			}else {
				proxyUri = "lb://" + serviceId;
			}
		}
		return proxyUri;
	}

	public void setProxyUri(String proxyUri) {
		if(proxyUri != null && proxyUri.endsWith("/")) {
			proxyUri = proxyUri.substring(0,proxyUri.length() - 1);
		}
		this.proxyUri = proxyUri;
	}

	public String getServiceBaseUrl() {
		return serviceBaseUrl;
	}

	public void setServiceBaseUrl(String serviceBaseUrl) {
		this.serviceBaseUrl = serviceBaseUrl;
	}


	public String getUriPrefix() {
		return uriPrefix;
	}

	public void setUriPrefix(String uriPrefix) {
		this.uriPrefix = uriPrefix;
	}

	public void setApiInfos(Map<String, ApiInfo> apiInfos) {
		this.apiInfos = apiInfos;
	}
	
	public Map<String, ApiInfo> getApiInfos() {
		return apiInfos;
	}
	
	@JsonIgnore
	public boolean isGateway() {
		return GlobalRuntimeContext.APPID.equalsIgnoreCase(getServiceId());
	}
	
	public void addApiInfo(ApiInfo apiInfo) {
		if(apiInfos == null)apiInfos = new HashMap<>();
		String resolveUri = BizSystemModule.resolveRealUri(this, apiInfo.getUri());
		apiInfo.setUri(resolveUri);
		String identifier = buildIdentifier(apiInfo.getMethod(), resolveUri);
		apiInfo.setIdentifier(identifier);
		apiInfos.put(identifier, apiInfo);
		if(identifier.contains("{")) {
			Pattern pattern = Pattern.compile(identifier.replaceAll("\\{[^/]+?\\}", ".+"));
			wildcardUris.put(pattern, apiInfo);
		}
	}

	public ApiInfo getApiInfo(String method, String uri) {
		String identifier = buildIdentifier(method, uri);
		ApiInfo apiInfo = getApiInfos().get(identifier);
		if(apiInfo != null) {
			return apiInfo;
		}
		for (Pattern pattern : sortedWildcardPatterns) {
			if(pattern.matcher(identifier).matches()) {
				return wildcardUris.get(pattern);
			}
		}
		return null;
	}
	
	public void updateOnApiListRefresh() {
		if(wildcardUris.isEmpty())return;
		//长的在前，避免最短匹配
		sortedWildcardPatterns = wildcardUris.keySet().stream().sorted((o1,o2) -> o2.pattern().length() - o1.pattern().length()).collect(Collectors.toList());
	}
	
	
	public String getMetadataUri() {
		if(getServiceBaseUrl() == null)return null;
		return getServiceBaseUrl() + "/exporter/metadata";
	}
	
	public String getHealthUri() {
		if(getServiceBaseUrl() == null)return null;
		return getServiceBaseUrl() + "/actuator/health";
	}
	
	public static String resolveRealUri(BizSystemModule module,String uri) {
		if(module.isGateway() || module.getStripPrefix() == 1) {
			if(uri.startsWith(GatewayConfigs.PATH_PREFIX + "/")) {
				return uri;
			}
			return GatewayConfigs.PATH_PREFIX + uri;
		}else {
			if(uri.startsWith(GatewayConfigs.PATH_PREFIX + "/" + module.getRouteName() + "/")) {
				return uri;
			}
			uri = String.format("%s/%s/%s", GatewayConfigs.PATH_PREFIX,module.getRouteName(),uri);
		}
		return uri.replace("//", "/");
	}
	
	public static String buildIdentifier(String method, String uri) {
		return new StringBuilder(method.toUpperCase()).append(GlobalConstants.UNDER_LINE).append(uri).toString();
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
				+ ", stripPrefix=" + stripPrefix + "]";
	}

	
	
}