package com.jeesuite.zuul.model;

import com.jeesuite.common.model.IdNamePair;

public class Tenant extends IdNamePair {

	private String type;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	
}
