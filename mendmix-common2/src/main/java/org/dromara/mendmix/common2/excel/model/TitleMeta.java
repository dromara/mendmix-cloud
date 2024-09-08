/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.common.excel.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月23日
 */
public class TitleMeta {

	private String title;
	
	private int rowIndex = 1;
	
	private int columnIndex;
	
	private Class<?> valueType;
	
	private TitleMeta parent;
	
	private List<TitleMeta> children;
	

	public TitleMeta(String title, int rowIndex, int columnIndex) {
		this.title = title;
		this.rowIndex = rowIndex;
		this.columnIndex = columnIndex;
	}
	
	public TitleMeta(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getRowIndex() {
		return rowIndex;
	}

	public void setRowIndex(int rowIndex) {
		this.rowIndex = rowIndex;
	}

	public int getColumnIndex() {
		return columnIndex;
	}

	public void setColumnIndex(int columnIndex) {
		this.columnIndex = columnIndex;
	}

	public TitleMeta getParent() {
		return parent;
	}

	public List<TitleMeta> getChildren() {
		return children == null ? (children = new ArrayList<TitleMeta>()) : children;
	}
	

	public Class<?> getValueType() {
		return valueType;
	}

	public void setValueType(Class<?> valueType) {
		this.valueType = valueType;
	}

	public void addChildren(TitleMeta child) {
		getChildren().add(child);
		child.parent = this;
	}

	@Override
	public String toString() {
		return "TitleCellBean [title=" + title + ", rowIndex=" + rowIndex + ", columnIndex=" + columnIndex + "]";
	}
	
	
}
