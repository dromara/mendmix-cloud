/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.common.http;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.GlobalConstants;
import com.mendmix.common.util.ResourceUtils;

/**
 * 
 * <br>
 * Class Name   : HostMappingHolder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年12月06日
 */
public class HostMappingHolder {

	private static final String PATH_SEPARATOR = "/";
	private static final String LOCALHOST = "localhost";

	private static Map<String, String> proxyUriMappings = new HashMap<>();
	private static Map<String, String> contextPathMappings = new HashMap<>();
	
	private static ProxyResolver proxyResolver;
	
	public static void setProxyResolver(ProxyResolver proxyResolver) {
		HostMappingHolder.proxyResolver = proxyResolver;
	}

	private static Map<String, String> getProxyUrlMappings() {
		if(!proxyUriMappings.isEmpty())return proxyUriMappings;
		synchronized (proxyUriMappings) {
			if(!proxyUriMappings.isEmpty())return proxyUriMappings;
			Map<String, String> mappings = ResourceUtils.getMappingValues("mendmix.loadbalancer.customize.mapping");
			//
			String proxyUrl;
			for (String serviceId : mappings.keySet()) {
				proxyUrl = fomatProxyUrl(mappings.get(serviceId));
				proxyUriMappings.put(serviceId.toLowerCase(), proxyUrl);
			}
			if(proxyUriMappings.isEmpty()){
				proxyUriMappings.put("mendmix", "mendmix");
			}
		}
		return proxyUriMappings;
	}
	
	private static Map<String, String> getContextPathMappings() {
		if(!contextPathMappings.isEmpty())return contextPathMappings;
		synchronized (contextPathMappings) {
			if(!contextPathMappings.isEmpty())return contextPathMappings;
			Map<String, String> mappingValues = ResourceUtils.getMappingValues("mendmix.loadbalancer.contextPath.mapping");
			contextPathMappings = new HashMap<>(mappingValues.size());
			String contextPath;
			for (String serviceId : mappingValues.keySet()) {
				contextPath = mappingValues.get(serviceId);
				if(!contextPath.startsWith(GlobalConstants.PATH_SEPARATOR)) {
					contextPath = GlobalConstants.PATH_SEPARATOR + contextPath;
				}
				contextPathMappings.put(serviceId, contextPath);
			}
			//
			if(contextPathMappings.isEmpty()) {
				contextPathMappings.put("mendmix", "mendmix");
			}
		}
		return contextPathMappings;
	}
	
	private static String fomatProxyUrl(String mappingUrl) {
		if(!mappingUrl.startsWith("http://") && !mappingUrl.startsWith("https://")) {
			mappingUrl = "http://" + mappingUrl;
		}
		if(mappingUrl.endsWith("/")) {
			mappingUrl = mappingUrl.substring(0,mappingUrl.length() - 1);
		}
		return mappingUrl;
	}
	
	public static String getProxyUrlMapping(String name){
		return getProxyUrlMappings().get(name.toLowerCase());
	}
	
	public static void addProxyUrlMapping(String name,String mappingUrl) {
		getProxyUrlMappings().put(name.toLowerCase(), mappingUrl);
	}
	
	public static boolean containsProxyUrlMapping(String name) {
		return getProxyUrlMappings().containsKey(name.toLowerCase());
	}
	
	public static String getContextPathMapping(String name){
		return getContextPathMappings().get(name.toLowerCase());
	}
	
	public static boolean containsContextPathMapping(String name) {
		return getContextPathMappings().containsKey(name.toLowerCase());
	}
	
	public static String resolveUrl(String url){
		String serviceId = StringUtils.split(url, PATH_SEPARATOR)[1].toLowerCase();
		if(serviceId.startsWith(LOCALHOST))return url;
		Map<String, String> baseNameMappings = getProxyUrlMappings();
		String realUrl = null;
		if(baseNameMappings.containsKey(serviceId)){
			realUrl = baseNameMappings.get(serviceId);
		}else if(proxyResolver != null && !serviceId.contains(GlobalConstants.DOT)) {//不是ip或者域名
			realUrl = proxyResolver.resolve(serviceId);	
		}
		
		if(realUrl != null) {
			if(containsContextPathMapping(serviceId)) {
				realUrl = realUrl + getContextPathMapping(serviceId);
			}
			String baseUrl = url.substring(0,url.indexOf(serviceId) + serviceId.length());
			return url.replace(baseUrl, realUrl);
		}

		return url;
	}
}
