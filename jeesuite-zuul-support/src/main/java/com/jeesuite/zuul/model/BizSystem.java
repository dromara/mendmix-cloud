package com.jeesuite.zuul.model;

import java.util.List;


public class BizSystem {

	private Integer id;
	/**
	 * 系统标识
	 */
	private String code;

	/**
	 * 系统名称
	 */
	private String name;

	/**
	 * 系统状态
	 */
	private Integer status;

	
	private List<BizSystemDomain> domains;
	
	private List<BizSystemModule> modules;
	

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public List<BizSystemDomain> getDomains() {
		return domains;
	}

	public void setDomains(List<BizSystemDomain> domains) {
		this.domains = domains;
	}

	public List<BizSystemModule> getModules() {
		return modules;
	}

	public void setModules(List<BizSystemModule> modules) {
		this.modules = modules;
	}

	
}
