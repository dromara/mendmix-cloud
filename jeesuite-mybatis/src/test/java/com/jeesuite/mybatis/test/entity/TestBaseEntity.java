package com.jeesuite.mybatis.test.entity;

import javax.persistence.Column;

import com.jeesuite.mybatis.core.BaseEntity;
import com.jeesuite.mybatis.plugin.autofield.annotation.CreatedAt;
import com.jeesuite.mybatis.plugin.autofield.annotation.CreatedBy;
import com.jeesuite.mybatis.plugin.autofield.annotation.UpdatedAt;
import com.jeesuite.mybatis.plugin.autofield.annotation.UpdatedBy;

/**
 * 
 * <br>
 * Class Name   : StandardBaseEntity
 */
public abstract class TestBaseEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@CreatedBy
    @Column(name = "created_by",updatable = false)
    private String createdBy;
	
	@CreatedAt
    @Column(name = "created_at",updatable = false)
    private java.util.Date createdAt;
	
	@UpdatedBy
    @Column(name = "updated_by",updatable = true)
    private String updatedBy;
	
	@UpdatedAt
    @Column(name = "updated_at",updatable = true)
    private java.util.Date updatedAt;

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    public java.util.Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.util.Date createdAt) {
        this.createdAt = createdAt;
    }
    public String getUpdatedBy() {
    	if(updatedBy == null)return createdBy;
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    public java.util.Date getUpdatedAt() {
    	if(updatedAt == null)return createdAt;
        return updatedAt;
    }

    public void setUpdatedAt(java.util.Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}