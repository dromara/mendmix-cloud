package com.jeesuite.mybatis.crud.helper;

import java.util.Map;
import java.util.Set;


public class EntityMapper {

    // 表
    private TableMapper tableMapper;

    // 全部列
    private Set<ColumnMapper> columnsMapper;

    // 主键
    private Set<ColumnMapper> idColumnsMapper;

    // 字段名和属性名的映射
    private Map<String, String> aliasMap;

    private Class<?> idClass;

    public TableMapper getTableMapper() {
        return tableMapper;
    }

    public void setTableMapper(TableMapper tableMapper) {
        this.tableMapper = tableMapper;
    }

    public Set<ColumnMapper> getColumnsMapper() {
        return columnsMapper;
    }

    public void setColumnsMapper(Set<ColumnMapper> columnsMapper) {
        this.columnsMapper = columnsMapper;
    }

    public Set<ColumnMapper> getIdColumnsMapper() {
        return idColumnsMapper;
    }

    public void setIdColumnsMapper(Set<ColumnMapper> idColumnsMapper) {
        this.idColumnsMapper = idColumnsMapper;
    }

    public Map<String, String> getAliasMap() {
        return aliasMap;
    }

    public void setAliasMap(Map<String, String> aliasMap) {
        this.aliasMap = aliasMap;
    }

    public Class<?> getIdClass() {
        return idClass;
    }

    public void setIdClass(Class<?> idClass) {
        this.idClass = idClass;
    }

}
