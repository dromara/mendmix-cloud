/**
 * 
 */
package com.mendmix.springweb.exporter.apidoc;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.mendmix.common.constants.ValueType;

/**
 * <br>
 * @author 姜维(00770874)
 * @date 2023年7月27日
 */
public class SimpleParameter {

	private String name;
	private String key;
	private boolean required;
	private ValueType type;
	private String defaultVal;
	private String remark;
	
	public SimpleParameter() {}

	public SimpleParameter(String name, String key) {
		this.name = name;
		this.key = key;
		this.type = ValueType.string;
	}
	
	public SimpleParameter(String name, String key, boolean required) {
		this(name, key);
		this.required = required;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}
	
	public ValueType getType() {
		return type;
	}

	public void setType(ValueType type) {
		this.type = type;
	}

	public String getDefaultVal() {
		return defaultVal;
	}

	public void setDefaultVal(String defaultVal) {
		this.defaultVal = defaultVal;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}
	
	
}
