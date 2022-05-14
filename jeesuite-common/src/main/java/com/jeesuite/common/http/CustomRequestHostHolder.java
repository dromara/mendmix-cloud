/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.jeesuite.common.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.GlobalConstants;
import com.jeesuite.common.util.ResourceUtils;

/**
 * 
 * <br>
 * Class Name   : CustomRequestHostResolver
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年12月06日
 */
public class CustomRequestHostHolder {

	private static final String PATH_SEPARATOR = "/";
	private static final String LOCALHOST = "localhost";

	private static Map<String, String> baseNameMappings = new HashMap<>();
	
	private static ProxyResolver proxyResolver;
	
	public static void setProxyResolver(ProxyResolver proxyResolver) {
		CustomRequestHostHolder.proxyResolver = proxyResolver;
	}

	public static void addMapping(String host,String mappingUrl) {
		getBaseNameMappings().put(host, mappingUrl);
	}
	
	private static Map<String, String> getBaseNameMappings() {
		if(!baseNameMappings.isEmpty())return baseNameMappings;
		synchronized (baseNameMappings) {
			if(!baseNameMappings.isEmpty())return baseNameMappings;
			Properties properties = ResourceUtils.getAllProperties("remote.baseurl.mapping");
			Set<Map.Entry<Object, Object>> entries = properties.entrySet();
			if (entries != null) {
				for (Map.Entry<Object, Object> entry : entries) {
					Object k = entry.getKey();
					Object v = entry.getValue();
					String[] parts = k.toString().split("\\[|\\]");
					String lbBaseUrl = parts[1];
					if(parts.length >= 4){
						lbBaseUrl = lbBaseUrl + ":" + parts[3];
					}
					baseNameMappings.put(lbBaseUrl.toLowerCase(), v.toString().replace("http://", "").replace("https://", ""));
				}
			}
			if(baseNameMappings.isEmpty()){
				baseNameMappings.put("x", "0");
			}
		}
		return baseNameMappings;
	}
	
	public static String getMapping(String name){
		return getBaseNameMappings().get(name);
	}
	
	public static String resolveUrl(String url){
		String lbBaseUrl = StringUtils.split(url, PATH_SEPARATOR)[1].toLowerCase();
		if(lbBaseUrl.startsWith(LOCALHOST))return url;
		Map<String, String> baseNameMappings = getBaseNameMappings();
		if(baseNameMappings.containsKey(lbBaseUrl)){
			return url.replace(lbBaseUrl, baseNameMappings.get(lbBaseUrl));
		}
		//不是ip或者域名
		if(proxyResolver != null && !lbBaseUrl.contains(GlobalConstants.DOT)) {
			String realSever = proxyResolver.resolve(lbBaseUrl);	
			if(realSever != null) {
				return url.replace(lbBaseUrl, realSever);
			}
		}
		return url;
	}
}
