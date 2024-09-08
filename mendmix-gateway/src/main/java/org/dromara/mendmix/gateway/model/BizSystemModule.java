/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.gateway.model;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.model.ApiInfo;
import org.dromara.mendmix.common.util.BeanUtils;
import org.dromara.mendmix.common.util.PathMatcher;
import org.dromara.mendmix.common.util.WebUtils;
import org.dromara.mendmix.gateway.GatewayConfigs;
import org.dromara.mendmix.gateway.GatewayConstants;
import org.dromara.mendmix.gateway.helper.RequestContextHelper;

public class BizSystemModule {
	
	private static Map<String, String> routeMetadataDefines = ImmutableMap.of("connectTimeout", "connect-timeout", "responseTimeout", "response-timeout");
	private String id;
    private String serviceId;

    private String routeName;

    private String proxyUri;
    
    private String anonymousUris;

    private String systemId;

    private String name;
    
    private boolean global; //
    
    private boolean local; //
    
    private int stripPrefix = -1;
    
    private boolean withMetadata; //
    
    private boolean stdResponse; //
    
    private boolean apiLogging; //模块是否已经开启日志收集
    
    private boolean subGateway; //
    
    private String rewriteUriPrefix;
    
    private boolean disabledApis;
    
    private Boolean ignoreApiPerm; //
    
    private String serviceBaseUrl;
    
    private String contextPath;
    
    private String uriPrefix;
    
    private Map<String, Object> metadata;
    
    private List<String> resolveRouteNames;
    
    private RouteDefinition routeDefinition;
    
    @JsonIgnore
    private Map<Pattern, ApiInfo> wildcardUris = new HashMap<>();

    private Map<String, ApiInfo> apiInfos;
    
    @JsonIgnore
    private PathMatcher anonymousUriMatcher = new PathMatcher();
    @JsonIgnore
    private List<Pattern> sortedWildcardPatterns = new ArrayList<>();

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

	@Deprecated
	public String getRouteName() {
		return routeName;
	}
	
	public String getCurrentRouteName(ServerWebExchange exchange) {
		return StringUtils.defaultString(RequestContextHelper.getCurrentRouteName(exchange), routeName);
	}
	
	public String getFirstRouteName() {
		if(getResolveRouteNames() == null || getResolveRouteNames().isEmpty())return null;
		return getResolveRouteNames().get(0);
	}

	public void setRouteName(String routeName) {
		this.routeName = StringUtils.trimToNull(routeName);
		if(this.routeName == null) {
			resolveRouteNames = new ArrayList<>(0);
		}else {
			List<String> subs = Arrays.asList(StringUtils.split(this.routeName, ","));
			resolveRouteNames = new ArrayList<>(subs.size());
			for (String sub : subs) {
				if(sub.startsWith("/")) {
					sub = sub.substring(1);
				}
				resolveRouteNames.add(sub);
			}
		}
	}
	
	public List<String> getResolveRouteNames() {
		return resolveRouteNames;
	}

	public void setResolveRouteNames(List<String> resolveRouteNames) {
		this.resolveRouteNames = resolveRouteNames;
		if(resolveRouteNames == null || resolveRouteNames.isEmpty()) {
			this.routeName = null;
		}else {
			this.routeName = StringUtils.join(resolveRouteNames, ",");
		}
	}

	public String getAnonymousUris() {
		return anonymousUris;
	}

	public void setAnonymousUris(String anonymousUris) {
		this.anonymousUris = anonymousUris;
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
	
	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

	public int getStripPrefix() {
		return stripPrefix;
	}

	public void setStripPrefix(int stripPrefix) {
		this.stripPrefix = stripPrefix;
	}
	
	public boolean isWithMetadata() {
		return withMetadata;
	}

	public void setWithMetadata(boolean withMetadata) {
		this.withMetadata = withMetadata;
	}
	
	public boolean isDisabledApis() {
		return disabledApis;
	}

	public void setDisabledApis(boolean disabledApis) {
		this.disabledApis = disabledApis;
	}

	public boolean isStdResponse() {
		return stdResponse;
	}

	public void setStdResponse(boolean stdResponse) {
		this.stdResponse = stdResponse;
	}
	
	public boolean isApiLogging() {
		return apiLogging;
	}

	public void setApiLogging(boolean apiLogging) {
		this.apiLogging = apiLogging;
	}

	public void setProxyUri(String proxyUri) {
		if(StringUtils.isNotBlank(proxyUri)) {
			//属性
			if(proxyUri.contains("?")) {
				String[] parts = StringUtils.split(proxyUri, "?", 2);
				proxyUri = parts[0];
				parts = StringUtils.split(parts[1], "&");
				String[] subParts;
				for (String part : parts) {
					subParts = StringUtils.split(part, "=", 2);
					if(subParts.length < 2)continue;
					getMetadata().put(subParts[0], subParts[1]);
				}
			}
			if(!proxyUri.contains("://")) {
				proxyUri = "http://" + proxyUri;
			}
			if(proxyUri.endsWith("/")) {
				proxyUri = proxyUri.substring(0, proxyUri.length() -1);
			}
		}
		this.proxyUri = StringUtils.trimToNull(proxyUri);
	}
	
	public String getProxyUri() {
		if(proxyUri == null && serviceId != null) {
			if(serviceId.contains(":")) {
				proxyUri = serviceId;
			}else {
				proxyUri = "lb://" + serviceId;
			}
		}
		return proxyUri;
	}
	
	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getServiceBaseUrl() {
		return serviceBaseUrl;
	}

	public void setServiceBaseUrl(String serviceBaseUrl) {
		this.serviceBaseUrl = serviceBaseUrl;
	}

	public String getUriPrefix() {
		if(uriPrefix == null) {
			if(isGateway() || (stripPrefix == 0 && getResolveRouteNames().size() > 1)) {
				return GatewayConfigs.PATH_PREFIX;
			}
			return uriPrefix = (GatewayConfigs.PATH_PREFIX + "/" + getFirstRouteName());
		}
		return uriPrefix;
	}

	public void setUriPrefix(String uriPrefix) {
		this.uriPrefix = uriPrefix;
	}
	
	public boolean isSubGateway() {
		return subGateway;
	}

	public void setSubGateway(boolean subGateway) {
		this.subGateway = subGateway;
	}
	
	public String getRewriteUriPrefix() {
		return rewriteUriPrefix;
	}

	public void setRewriteUriPrefix(String rewriteUriPrefix) {
		this.rewriteUriPrefix = rewriteUriPrefix;
	}

	public boolean isIgnoreApiPerm() {
		return ignoreApiPerm == null ? subGateway : ignoreApiPerm;
	}

	public void setIgnoreApiPerm(boolean ignoreApiPerm) {
		this.ignoreApiPerm = ignoreApiPerm;
	}

	public Map<String, Object> getMetadata() {
		return metadata == null ? (metadata = new HashMap<>()) : metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public void setApiInfos(Map<String, ApiInfo> apiInfos) {
		this.apiInfos = apiInfos;
	}
	
	public Map<String, ApiInfo> getApiInfos() {
		return apiInfos;
	}

	public RouteDefinition getRouteDefinition() {
		return routeDefinition;
	}

	public void setRouteDefinition(RouteDefinition routeDefinition) {
		this.routeDefinition = routeDefinition;
	}

	@JsonIgnore
	public boolean isGateway() {
		return GlobalContext.APPID.equals(getFirstRouteName());
	}
	
	public void updateOnApiListRefresh() {
		if(wildcardUris.isEmpty())return;
		//长的在前，避免最短匹配
		sortedWildcardPatterns = wildcardUris.keySet().stream().sorted((o1,o2) -> o2.pattern().length() - o1.pattern().length()).collect(Collectors.toList());
	}

	public void addApiInfo(ApiInfo apiInfo) {
		if(apiInfos == null) {
			apiInfos = new HashMap<>();
		}
		if(StringUtils.isAnyBlank(apiInfo.getMethod(),apiInfo.getUri())) {
			System.err.println(String.format("✘✘✘✘✘ Ignore_nullvalue_api -> routeName:%s,apiInfo:%s", routeName,apiInfo));
			return;
		}
		String uniqueKey = buildApiIdentifier(apiInfo.getMethod(), resolveFullUri(this, apiInfo.getUri()));
		//
		String identifier;
		String uri = apiInfo.getUri();
		if(isGateway()) {
			if(uri.startsWith(GatewayConfigs.PATH_PREFIX + "/")) {
				uri = apiInfo.getUri().substring(GatewayConfigs.PATH_PREFIX.length());
			}
			identifier = buildApiIdentifier(apiInfo.getMethod(), uri);
		}else if(getStripPrefix() == 1) {
			if(!uri.startsWith("/" + getFirstRouteName() + "/")) {
				uri = "/" + getFirstRouteName() + uri;
			}
			identifier = buildApiIdentifier(apiInfo.getMethod(), uri);
		}else {
			identifier = buildApiIdentifier(apiInfo.getMethod(), uri);
		}
		apiInfo.setIdentifier(identifier);
		apiInfo.setRoutedIdentifier(uniqueKey);
		apiInfos.put(uniqueKey, apiInfo);
		if(uniqueKey.contains("{")) {
			Pattern pattern = Pattern.compile(uniqueKey.replaceAll("\\{[^/]+?\\}", ".+"));
			wildcardUris.put(pattern, apiInfo);
		}
	}
	
	public ApiInfo getApiInfo(String method, String uri) {
		if(apiInfos == null)return null;
		String uniqueKey = buildApiIdentifier(method, uri);
		ApiInfo apiInfo = apiInfos.get(uniqueKey);
		if(apiInfo != null) {
			return apiInfo;
		}
		for (Pattern pattern : sortedWildcardPatterns) {
			if(pattern.matcher(uniqueKey).matches()) {
				return wildcardUris.get(pattern);
			}
		}
		return null;
	}
	
	public static String resolveFullUri(BizSystemModule module,String uri) {
		String routePath = "/" + module.getFirstRouteName() + "/";
		if(module.isGateway()) {
			if(uri.startsWith(GatewayConfigs.PATH_PREFIX + "/")) {
				return uri;
			}
			return GatewayConfigs.PATH_PREFIX + uri;
		}else if(module.getStripPrefix() == 1) {
			if(uri.startsWith(module.getUriPrefix() + "/")) {
				return uri;
			}else if(uri.startsWith(routePath)) {
				return GatewayConfigs.PATH_PREFIX + uri;
			}
			return module.getUriPrefix() + uri;
		}else {
			if(uri.startsWith(module.getUriPrefix())) {
				return uri;
			}
			if(module.contextPath != null && uri.startsWith(module.contextPath)) {
				uri = uri.substring(module.contextPath.length() + 1);
			}
			uri = String.format("%s/%s",module.getUriPrefix(),uri);
		}
		return uri.replace("//", "/");
	}
	
	public static String buildApiIdentifier(String method, String uri) {
		return new StringBuilder().append(method).append(GlobalConstants.UNDER_LINE).append(uri).toString();
	}
	
	
	public String getMetadataUri() {
		//代理网关：云边需要拉取对应的api定义
		if(getServiceBaseUrl() == null || isSubGateway())return null;
		return getServiceBaseUrl() + "/metadata";
	}
	
	public String getInfoUri() {
		//代理网关：云边需要拉取对应的api定义
		if(getServiceBaseUrl() == null || isSubGateway())return null;
		return getServiceBaseUrl() + "/exporter/info";
	}
	
	public String getHealthUri() {
		if(getServiceBaseUrl() == null)return null;
		return getServiceBaseUrl() + "/actuator/health";
	}
	
	public boolean isGlobalAnonymous(String uri) {
		return anonymousUriMatcher.match(uri);
	}
	
	public void format() {
		String firstRoute = getResolveRouteNames().get(0);
        if(getMetadata().containsKey("StripPrefix")) {
        	getMetadata().put("stripPrefix", getMetadata().remove("StripPrefix"));
		}
        if(!getMetadata().isEmpty()) {
        	BeanUtils.copy(getMetadata(), this);
    		Field[] fields = FieldUtils.getAllFields(this.getClass());
    		for (Field field : fields) {
    			getMetadata().remove(field.getName());
			}
        }
        
        if(!getMetadata().isEmpty()) {
        	Map<String, Object> _metadata = new HashMap<>();
            getMetadata().forEach((k,v) -> {
            	if(routeMetadataDefines.containsKey(k)) {
            		_metadata.put(routeMetadataDefines.get(k), v);
            	}else {
            		_metadata.put(k, v);
            	}
            });
            this.metadata = _metadata;
        }
        //
		if(StringUtils.countMatches(this.proxyUri, "/") > 2) {
			String removeSchema = StringUtils.splitByWholeSeparator(this.proxyUri, "://")[1];
			contextPath = removeSchema.substring(removeSchema.indexOf("/"));
		}
        
		if(stripPrefix < 0) {
			//route:spu,proxyUri:lb://mall-product-svc
			//route:spu,proxyUri:lb://mall-product-svc/spu
			//route:mall/spu,proxyUri:lb://mall-product-svc/spu
			//route:api/product,api/order,proxyUri:lb://mall-product-svc
			this.stripPrefix = StringUtils.countMatches(firstRoute, "/") + 1; //route部分
			if(StringUtils.isNotBlank(GatewayConfigs.PATH_PREFIX)) {
				this.stripPrefix = this.stripPrefix + StringUtils.countMatches(GatewayConfigs.PATH_PREFIX, "/");
			}
			//代理地址带contextpath的情况
			if(contextPath != null) {
				this.stripPrefix = this.stripPrefix - StringUtils.countMatches(firstRoute.replace(contextPath, ""), "/") - 1;
			}
		}
		//
		if(!isGateway() && !getProxyUri().contains("ws://")) {
			if(getProxyUri().startsWith("http")) {
				serviceBaseUrl = getProxyUri();
			}else if(getProxyUri().startsWith("lb://")) {
				serviceBaseUrl = getProxyUri().replace("lb://", "http://");
			}
			if(serviceBaseUrl != null) {
				String rootUrl = WebUtils.getBaseUrl(serviceBaseUrl);
				if(stripPrefix == 0) {
					serviceBaseUrl = rootUrl + GatewayConfigs.PATH_PREFIX;
				}else{
					int fromPathIndex = stripPrefix - StringUtils.countMatches(GatewayConfigs.PATH_PREFIX, "/");
				   String[] routeParts = StringUtils.split(getFirstRouteName(), "/");
				   serviceBaseUrl = rootUrl;
				   boolean buildContextPath = contextPath == null;
				   for (int i = fromPathIndex; i < routeParts.length; i++) {
					   serviceBaseUrl = serviceBaseUrl + "/" + routeParts[i];
					   if(buildContextPath) {
						   contextPath = StringUtils.trimToEmpty(contextPath) + "/" + routeParts[i];
					   }
				   }
				}
			}
		}
		//
		if(!isGateway() && !proxyUri.contains("ws://")){
			URI uri = URI.create(proxyUri);
			this.proxyUri = uri.getScheme() + "://" + uri.getAuthority();
		}
		//
		if(contextPath != null && rewriteUriPrefix == null) {
			if(isSubGateway()) {
				if(!GatewayConfigs.PATH_PREFIX.equals(contextPath) && stripPrefix == 0) {
					this.rewriteUriPrefix = contextPath;
				}
			}else if(!getUriPrefix().endsWith(contextPath)){
				this.rewriteUriPrefix = contextPath;
			}
		}
		//
		formatGlobalAnonymousUris();
	}
	

	private void formatGlobalAnonymousUris() {
		//配置的匿名接口
		if(isGateway()) {
			anonymousUriMatcher.addUriPattern(GatewayConfigs.PATH_PREFIX, "/error");
			//logout 之前代码代码配置的是需要登录，统一设置未匿名
			anonymousUriMatcher.addUriPattern(GatewayConfigs.PATH_PREFIX, "/logout");
		}
		if(anonymousUris != null) {
			List<String> uriPatterns = new ArrayList<>(Arrays.asList(StringUtils.split(anonymousUris, ";,")));
			String fullPattern;
			for (String uriPattern : uriPatterns) {
				if(StringUtils.isBlank(uriPattern))continue;
				fullPattern = resolveFullUri(this, uriPattern);
				anonymousUriMatcher.addUriPattern("", fullPattern);
			}
		}
	}

	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((routeName == null) ? 0 : routeName.hashCode());
		result = prime * result + ((serviceId == null) ? 0 : serviceId.hashCode());
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
		if (routeName == null) {
			if (other.routeName != null)
				return false;
		} else if (!routeName.equals(other.routeName))
			return false;
		if (serviceId == null) {
			if (other.serviceId != null)
				return false;
		} else if (!serviceId.equals(other.serviceId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BizSystemModule [id=" + id + ", serviceId=" + serviceId + ", routeName=" + routeName + ", stripPrefix="
				+ stripPrefix + ", global=" + global + ", contextPath=" + contextPath + ", uriPrefix=" + uriPrefix
				+ "]";
	}

	

	
}