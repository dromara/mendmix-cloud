package org.dromara.mendmix.example.dao.entity;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dromara.mendmix.example.dao.ExampleBaseEntity;

@Table(name = "department")
public class ExampleDepartmentEntity extends ExampleBaseEntity {
    @Id
    @SequenceGenerator(name="",sequenceName="true")
    private Integer id;

    /**
     * 主数据编码
     */
    private String code;

    /**
     * 组织名称
     */
    private String name;

    /**
     * 父级ID
     */
    @Column(name = "parent_id")
    private String parentId;

    /**
     * 组织类型
     */
    @Column(name = "org_type")
    private String orgType;

    /**
     * 排序
     */
    private Integer sort;

    @Column(name = "company_id")
    private Integer companyId;

    /**
     * 是否启用
     */
    private Boolean enabled;


    /**
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 获取主数据编码
     *
     * @return code - 主数据编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置主数据编码
     *
     * @param code 主数据编码
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 获取组织名称
     *
     * @return name - 组织名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置组织名称
     *
     * @param name 组织名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取父级ID
     *
     * @return parent_id - 父级ID
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * 设置父级ID
     *
     * @param parentId 父级ID
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     * 获取组织类型
     *
     * @return org_type - 组织类型
     */
    public String getOrgType() {
        return orgType;
    }

    /**
     * 设置组织类型
     *
     * @param orgType 组织类型
     */
    public void setOrgType(String orgType) {
        this.orgType = orgType;
    }

    /**
     * 获取排序
     *
     * @return sort - 排序
     */
    public Integer getSort() {
        return sort;
    }

    /**
     * 设置排序
     *
     * @param sort 排序
     */
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    /**
     * @return company_id
     */
    public Integer getCompanyId() {
        return companyId;
    }

    /**
     * @param companyId
     */
    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    /**
     * 获取是否启用
     *
     * @return enabled - 是否启用
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用
     *
     * @param enabled 是否启用
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

}