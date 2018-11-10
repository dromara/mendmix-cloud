package com.jeesuite.common.model;

public class SelectOption {

	private String value;
	private String text;
	
	public SelectOption() {}
	
	public SelectOption(String value, String text) {
		super();
		this.value = value;
		this.text = text;
	}

	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	
	
	
}
