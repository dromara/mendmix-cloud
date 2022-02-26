package com.jeesuite.gateway.model;

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

	
	private List<BizSystemPortal> portals;
	
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


	public List<BizSystemPortal> getPortals() {
		return portals;
	}

	public void setPortals(List<BizSystemPortal> portals) {
		this.portals = portals;
	}

	public List<BizSystemModule> getModules() {
		return modules;
	}

	public void setModules(List<BizSystemModule> modules) {
		this.modules = modules;
	}

	
}
