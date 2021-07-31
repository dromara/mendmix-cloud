package com.jeesuite.zuul.model;

import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.jeesuite.common.json.deserializer.DateTimeConvertDeserializer;
import com.jeesuite.common.json.serializer.DateTimeConvertSerializer;

/**
 * 
 * <br>
 * Class Name   : Role
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月21日
 */
public class Role {

	private Integer id;
	private String name;
	private Integer systemId;
	private String tenantId;
	private String departmentId;
	private String departmentName;
	private String departmentLongCode;
	private String remarks;
	private Boolean enabled;
	private String updatedBy;
	@JsonSerialize(using = DateTimeConvertSerializer.class) 
    @JsonDeserialize(using = DateTimeConvertDeserializer.class)
	private Date updatedAt;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public Integer getSystemId() {
		return systemId;
	}
	public void setSystemId(Integer systemId) {
		this.systemId = systemId;
	}
	public String getRemarks() {
		return remarks;
	}
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
	public String getTenantId() {
		return tenantId;
	}
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	public String getDepartmentId() {
		return departmentId;
	}
	public void setDepartmentId(String departmentId) {
		this.departmentId = departmentId;
	}
	public String getDepartmentName() {
		return departmentName;
	}
	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}
	
	public String getDepartmentLongCode() {
		return departmentLongCode;
	}
	public void setDepartmentLongCode(String departmentLongCode) {
		this.departmentLongCode = departmentLongCode;
	}
	
	public Boolean getEnabled() {
		return enabled;
	}
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	public String getUpdatedBy() {
		return updatedBy;
	}
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
	public Date getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

}
