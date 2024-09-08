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

/**
 * 
 * <br>
 * Class Name   : PageQueryRequest
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年3月23日
 */
public class PageQueryRequest<T> {

	private int pageNo = 1;
	private int pageSize = 15;
	private OrderBy orderBy;
	private T example;
	
	public PageQueryRequest() {}
	
	public PageQueryRequest(int pageNo, int pageSize, OrderBy orderBy, T example) {
		super();
		this.pageNo = pageNo;
		this.pageSize = pageSize;
		this.orderBy = orderBy;
		this.example = example;
	}

	public PageQueryRequest(int pageNo, int pageSize, T example) {
		super();
		this.pageNo = pageNo;
		this.pageSize = pageSize;
		this.example = example;
	}

	public int getPageNo() {
		return pageNo;
	}
	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}
	public int getPageSize() {
		return pageSize;
	}
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	public T getExample() {
		return example;
	}
	public void setExample(T example) {
		this.example = example;
	}
	public OrderBy getOrderBy() {
		return orderBy;
	}
	public void setOrderBy(OrderBy orderBy) {
		this.orderBy = orderBy;
	}
	
	public PageParams asPageParam() {
		return new PageParams(pageNo <= 0 ? 1 : pageNo, pageSize);
	}

}
