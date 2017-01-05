/**
 * 
 */
package com.jeesuite.common2.excel.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月23日
 */
public class TitleCellBean {

	private String title;
	
	private int rowIndex;
	
	private int columnIndex;
	
	private TitleCellBean parent;
	
	private List<TitleCellBean> children;
	

	public TitleCellBean(String title, int rowIndex, int columnIndex) {
		this.title = title;
		this.rowIndex = rowIndex;
		this.columnIndex = columnIndex;
	}
	
	public TitleCellBean(String title) {
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

	public TitleCellBean getParent() {
		return parent;
	}

	public List<TitleCellBean> getChildren() {
		return children == null ? (children = new ArrayList<TitleCellBean>()) : children;
	}

	public void addChildren(TitleCellBean child) {
		getChildren().add(child);
		child.parent = this;
	}

	@Override
	public String toString() {
		return "TitleCellBean [title=" + title + ", rowIndex=" + rowIndex + ", columnIndex=" + columnIndex + "]";
	}
	
	
}
