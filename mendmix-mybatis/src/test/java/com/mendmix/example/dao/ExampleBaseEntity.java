/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.example.dao;

import javax.persistence.Column;

import com.mendmix.mybatis.core.BaseEntity;
import com.mendmix.mybatis.plugin.autofield.annotation.CreatedAt;
import com.mendmix.mybatis.plugin.autofield.annotation.CreatedBy;
import com.mendmix.mybatis.plugin.autofield.annotation.UpdatedAt;
import com.mendmix.mybatis.plugin.autofield.annotation.UpdatedBy;

/**
 * 
 * <br>
 * Class Name   : ExampleBaseEntity
 */
public abstract class ExampleBaseEntity extends BaseEntity {

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
	
	/**
     * 删除状态（0：否，1：已删除）
     */
    private Boolean deleted;

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

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}
    
    
}