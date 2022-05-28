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
package com.mendmix.common.model;

/**
 * 
 * <br>
 * Class Name : OrderBy
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年3月23日
 */
public class OrderBy {

	public static enum OrderType {
		DESC, ASC
	}

	private String field;
	private String sortType; // DESC,ASC

	public OrderBy() {
	}

	public OrderBy(String field) {
		this(field, OrderType.ASC);
	}

	public OrderBy(String field, String sortType) {
		this.field = field;
		this.sortType = sortType;
	}

	public OrderBy(String field, OrderType sortType) {
		this.field = field;
		this.sortType = sortType.name();
	}

	/**
	 * @return the field
	 */
	public String getField() {
		return field;
	}

	/**
	 * @param field
	 *            the field to set
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
	 * @param sortType
	 *            the sortType to set
	 */
	public void setSortType(String sortType) {
		this.sortType = sortType;
	}

}
