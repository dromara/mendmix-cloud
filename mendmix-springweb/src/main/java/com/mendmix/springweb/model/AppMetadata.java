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
package com.mendmix.springweb.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mendmix.common.model.ApiInfo;

public class AppMetadata {

	private String name;
	private String group;
	private String module;
	private String serviceId;
	private List<String> dependencyServices = new ArrayList<>(0);
	private Map<String,ApiInfo> apis = new HashMap<>();
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
	
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public String getModule() {
		return module;
	}
	public void setModule(String module) {
		this.module = module;
	}
	
	/**
	 * @return the serviceId
	 */
	public String getServiceId() {
		return serviceId;
	}
	/**
	 * @param serviceId the serviceId to set
	 */
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
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
	 * @return the apis
	 */
	public Collection<ApiInfo> getApis() {
		return apis.values();
	}
	/**
	 * @param apis the apis to set
	 */
	public void setApis(List<ApiInfo> apis) {
		for (ApiInfo api : apis) {
			addApi(api);
		}
	}
	
	public void addApi(ApiInfo api) {
		this.apis.put(api.getUrl(), api);
	}

}
