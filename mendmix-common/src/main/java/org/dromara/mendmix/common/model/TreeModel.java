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
package org.dromara.mendmix.common.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class TreeModel {

	private String id;
	private String name;
	private String value;
	private String parentId;
	private boolean checked = false;
	private boolean disabled = false;
	private int sort;
	private List<TreeModel> children;
	

	public TreeModel() {}
	
	
	public TreeModel(String id, String name, String value,String pid) {
		super();
		this.id = id;
		this.name = name;
		this.value = value;
		this.parentId = pid;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getValue() {
		return value;
	}


	public void setValue(String value) {
		this.value = value;
	}

	

	public String getParentId() {
		return parentId;
	}


	public void setParentId(String parentId) {
		this.parentId = parentId;
	}


	public boolean isChecked() {
		return checked;
	}


	public void setChecked(boolean checked) {
		this.checked = checked;
	}


	public boolean isDisabled() {
		return disabled;
	}


	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}


	public int getSort() {
		return sort;
	}


	public void setSort(int sort) {
		this.sort = sort;
	}


	public void addChild(TreeModel child) {
		children = children == null ? (children = new ArrayList<>()) : children;
		children.add(child);
	}
	
	public List<TreeModel> getChildren() {
		return children;
	}
	public void setChildren(List<TreeModel> children) {
		this.children = children;
	}
	
	public boolean isLeaf() {
		return this.children != null;
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TreeModel other = (TreeModel) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}


	/**
	 * 
	 * @param models 已排序
	 * @return
	 */
	public static <T extends TreeModel> TreeModel build(List<T> models){
		TreeModel root = new TreeModel();
		if(models.isEmpty()){
			root.children = new ArrayList<>(0);
			return root;
		}
		
		Map<String, TreeModel> modelMap = new HashMap<>();
		for (TreeModel model : models) {
			if(!model.isLeaf()){
				modelMap.put(model.getId(), model);
			}
		}
		for (TreeModel model : models) {
			if(((StringUtils.isBlank(model.getParentId())) && !model.isLeaf()) || !modelMap.containsKey(model.getParentId())){
				root.addChild(model);
			}else{
				modelMap.get(model.getParentId()).addChild(model);
			}
		}
		
		sortedChildren(root);
		
		return root;
	}
	
	private static void sortedChildren(TreeModel model) {
		List<TreeModel> children = model.getChildren();
		if(children == null || children.isEmpty())return;
		children.sort(Comparator.comparingInt(TreeModel::getSort));
		for (TreeModel child : children) {
			sortedChildren(child);
		}
	}

}
