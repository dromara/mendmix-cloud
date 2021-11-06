package com.jeesuite.springweb.model;

import java.util.ArrayList;
import java.util.List;

public class AppMetadata {

	private String name;
	private String group;
	private String module;
	private String serviceId;
	private List<String> dependencyServices = new ArrayList<>(0);
	private List<ApiInfo> apis = new ArrayList<>();
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
	public List<ApiInfo> getApis() {
		return apis;
	}
	/**
	 * @param apis the apis to set
	 */
	public void setApis(List<ApiInfo> apis) {
		this.apis = apis;
	}
	
	

}
