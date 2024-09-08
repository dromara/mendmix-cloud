/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.mybatis.metadata;

import java.util.Set;

import javax.persistence.GenerationType;


public class EntityMetadata {

    // 表
    private TableMetadata tableMapper;

    // 全部列
    private Set<ColumnMetadata> columnsMapper;

    // 主键
    private ColumnMetadata idColumn;

    private Class<?> idClass;
    
    private GenerationType idStrategy;

    public TableMetadata getTableMapper() {
        return tableMapper;
    }

    public void setTableMapper(TableMetadata tableMapper) {
        this.tableMapper = tableMapper;
    }

    public Set<ColumnMetadata> getColumnsMapper() {
        return columnsMapper;
    }

    public void setColumnsMapper(Set<ColumnMetadata> columnsMapper) {
        this.columnsMapper = columnsMapper;
    }


    public ColumnMetadata getIdColumn() {
		return idColumn;
	}

	public void setIdColumn(ColumnMetadata idColumn) {
		this.idColumn = idColumn;
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
