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
package com.mendmix.mybatis.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.GenerationType;


public class EntityMetadata {

    // 表
    private TableMetadata table;

    // 全部列
    private Set<ColumnMetadata> columns;

    // 主键
    private ColumnMetadata idColumn;

    // 属性字段->列名
    private Map<String, String> prop2ColumnMappings = new HashMap<>();

    private Class<?> idClass;
    
    private GenerationType idStrategy;

    public TableMetadata getTable() {
        return table;
    }

    public void setTable(TableMetadata tableMeta) {
        this.table = tableMeta;
    }

    public Set<ColumnMetadata> getColumns() {
        return columns;
    }

    public void setColumns(Set<ColumnMetadata> columns) {
        this.columns = columns;
        for (ColumnMetadata column : columns) {
			prop2ColumnMappings.put(column.getProperty(), column.getColumn());
		}
    }


    public ColumnMetadata getIdColumn() {
		return idColumn;
	}

	public void setIdColumn(ColumnMetadata idColumn) {
		this.idColumn = idColumn;
	}

	

    public Map<String, String> getProp2ColumnMappings() {
		return prop2ColumnMappings;
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
		return idStrategy != null;
	}

}
