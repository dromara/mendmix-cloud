package com.jeesuite.mybatis.crud.helper;

import java.util.Map;
import java.util.Set;

import javax.persistence.GenerationType;


public class EntityMapper {

    // 表
    private TableMapper tableMapper;

    // 全部列
    private Set<ColumnMapper> columnsMapper;

    // 主键
    private ColumnMapper idColumn;

    // 字段名和属性名的映射
    private Map<String, String> aliasMap;

    private Class<?> idClass;
    
    private GenerationType idStrategy;

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


    public ColumnMapper getIdColumn() {
		return idColumn;
	}

	public void setIdColumn(ColumnMapper idColumn) {
		this.idColumn = idColumn;
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

	public GenerationType getIdStrategy() {
		return idStrategy;
	}

	public void setIdStrategy(GenerationType idStrategy) {
		this.idStrategy = idStrategy;
	}
   
	public boolean autoId(){
		return idStrategy == GenerationType.AUTO;
	}

}
