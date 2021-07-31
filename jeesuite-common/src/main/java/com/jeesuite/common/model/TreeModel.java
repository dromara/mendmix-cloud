package com.jeesuite.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class TreeModel {

	private String id;
	private String name;
	private String value;
	private String icon;
	private String pid;
	private Object data;
	private boolean checked = false;
	private boolean disabled = false;
	private List<TreeModel> children;
	

	public TreeModel() {}
	
	
	public TreeModel(String id, String name, String value,String pid) {
		super();
		this.id = id;
		this.name = name;
		this.value = value;
		this.pid = pid;
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


	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public String getPid() {
		return pid;
	}
	public void setPid(String pid) {
		this.pid = pid;
	}

	/**
	 * @return the data
	 */
	public Object getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(Object data) {
		this.data = data;
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
		return StringUtils.isNotBlank(value);
	}
	
	@SuppressWarnings("unchecked")
	public TreeModel addDataItem(String key,Object value){
		if(data == null){
			data = new HashMap<String,Object>();
		}
		((Map)data).put(key, value);
		return this;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		return true;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}


	/**
	 * 
	 * @param models 已排序
	 * @return
	 */
	public static TreeModel build(List<TreeModel> models){
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
			if(((StringUtils.isBlank(model.getPid())) && !model.isLeaf()) || !modelMap.containsKey(model.getPid())){
				root.addChild(model);
			}else{
				modelMap.get(model.getPid()).addChild(model);
			}
		}
		
		return root;
	}
	

}
