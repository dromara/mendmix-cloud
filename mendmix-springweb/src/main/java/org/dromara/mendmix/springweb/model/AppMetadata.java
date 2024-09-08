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
package org.dromara.mendmix.springweb.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.model.ApiInfo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AppMetadata {

	private String name;
	private String system;
	private String module;
	private boolean stdResponse;
	private boolean apiLogging;
	private List<String> dependencyServices = new ArrayList<>(0);
	private Map<String,ApiInfo> apis = new HashMap<>();
	
	@JsonIgnore
    private Map<Pattern, ApiInfo> wildcardUris = new HashMap<>();
	@JsonIgnore
    private List<Pattern> sortedWildcardPatterns = new ArrayList<>();
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	public String getSystem() {
		return system;
	}
	public void setSystem(String system) {
		this.system = system;
	}
	public String getModule() {
		return module;
	}
	public void setModule(String module) {
		this.module = module;
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
	/**
	 * @return the dependencyServices
	 */
	public List<String> getDependencyServices() {
		return dependencyServices;
	}
	
	/**
	 * @param dependencyServices the dependencyServices to set
	 */
	public void setDependencyServices(List<String> dependencyServices) {
		this.dependencyServices = dependencyServices;
	}

	/**
	 * @param apis the apis to set
	 */
	public void addApi(ApiInfo api) {
		final String identifier = api.getIdentifier();
		this.apis.put(identifier, api);
		if(identifier.contains("{")) {
			Pattern pattern = Pattern.compile(identifier.replaceAll("\\{[^/]+?\\}", ".+"));
			wildcardUris.put(pattern, api);
		}
	}
	
	public void setApis(List<ApiInfo> apis) {
		if(apis == null) return;
		for (ApiInfo api : apis) {
			addApi(api);
		}
	}
	
	public List<ApiInfo> getApis() {
		return new ArrayList<>(apis.values());
	}
	

	public ApiInfo getApi(String method,String uri) {
		String identifier = new StringBuilder(method.toUpperCase()).append(GlobalConstants.UNDER_LINE).append(uri).toString();
		ApiInfo apiInfo = apis.get(identifier);
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
	
	public void onInitFinished() {
		if(wildcardUris.isEmpty())return;
		//长的在前，避免最短匹配
		sortedWildcardPatterns = wildcardUris.keySet().stream().sorted((o1,o2) -> o2.pattern().length() - o1.pattern().length()).collect(Collectors.toList());
	}
	
	public void clearApis() {
		apis.clear();
	}
}
