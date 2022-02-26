package com.jeesuite.springweb.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jeesuite.common.model.ApiInfo;

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
