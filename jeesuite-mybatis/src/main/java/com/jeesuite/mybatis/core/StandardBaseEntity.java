package com.jeesuite.mybatis.core;

import java.util.Date;

import javax.persistence.Column;

/**
 * 标准结构基础实体类
 * 
 * <br>
 * Class Name   : StandardBaseEntity
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Oct 31, 2020
 */
public abstract class StandardBaseEntity extends BaseEntity {

	/**
	 * @return enabled 是否启用
	 */
	@Column(name = "enabled")
	private Boolean enabled = true;

	/**
	 * @return deleted 删除状态（0：否，1：已删除）
	 */
	@Column(name = "deleted")
	private Boolean deleted = false;
	
	
	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	/**
	 * @return createdAt
	 */
	@Column(name = "created_at", updatable = false)
	private java.util.Date createdAt;

	/**
	 * @return createdBy
	 */
	@Column(name = "created_by", updatable = false)
	private String createdBy;

	/**
	 * @return updatedAt 更新时间
	 */
	@Column(name = "updated_at")
	private java.util.Date updatedAt;

	/**
	 * @return updatedBy
	 */
	@Column(name = "updated_by")
	private String updatedBy;

	/**
	 * @return the createdAt
	 */
	public java.util.Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * @param createdAt
	 *            the createdAt to set
	 */
	public void setCreatedAt(java.util.Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * @return the createdBy
	 */
	public String getCreatedBy() {
		return createdBy;
	}

	/**
	 * @param createdBy
	 *            the createdBy to set
	 */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	/**
	 * @return the updatedAt
	 */
	public java.util.Date getUpdatedAt() {
		return updatedAt == null ? createdAt : updatedAt;
	}

	/**
	 * @param updatedAt
	 *            the updatedAt to set
	 */
	public void setUpdatedAt(java.util.Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	/**
	 * @return the updatedBy
	 */
	public String getUpdatedBy() {
		return updatedBy == null ? createdBy : updatedBy;
	}

	/**
	 * @param updatedBy
	 *            the updatedBy to set
	 */
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public void initCreated(String createdBy) {
		this.createdAt = new Date();
		this.createdBy = createdBy;
	}

}
