package com.jeesuite.common.model;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class KeyValues implements Serializable {

	private static final long serialVersionUID = 1L;

	private String key;
	private List<String> values;
	
	
	public KeyValues() {}
	
	
	public KeyValues(String key, List<String> values) {
		super();
		this.key = key;
		this.values = values;
	}



	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public List<String> getValues() {
		return values;
	}
	public void setValues(List<String> values) {
		this.values = values;
	}


	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}
	
	
}
