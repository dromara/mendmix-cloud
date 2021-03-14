package com.jeesuite.common.model;

public class IdNamePair {

	private String id;
	private String name;
	
	public IdNamePair() {}
	
	public IdNamePair(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	
}
