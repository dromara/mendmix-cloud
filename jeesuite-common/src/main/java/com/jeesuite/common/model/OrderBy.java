package com.jeesuite.common.model;

/**
 * 
 * <br>
 * Class Name   : OrderBy
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年3月23日
 */
public class OrderBy {

	private String field;
	private String sortType; //DESC,ASC
	/**
	 * @return the field
	 */
	public String getField() {
		return field;
	}
	/**
	 * @param field the field to set
	 */
	public void setField(String field) {
		this.field = field;
	}
	/**
	 * @return the sortType
	 */
	public String getSortType() {
		return sortType;
	}
	/**
	 * @param sortType the sortType to set
	 */
	public void setSortType(String sortType) {
		this.sortType = sortType;
	}
	
	
}
